package endfieldindustrylib.EFworld.blocks.AICTransport;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.GridBits;
import arc.struct.PQueue;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Structs;
import arc.util.io.Reads;
import arc.util.io.Writes;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.GenericAICBasicFacility;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.RectGenericAICBasicFacility;
import mindustry.Vars;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.meta.BlockGroup;

public class TransportBelt extends Conveyor {

    // ============================================================
    // 二段式点击建造：静态状态
    // ============================================================
    private static boolean configuring = false;
    private static int configStartX, configStartY;
    private static int configSourceX = -1, configSourceY = -1; // 起点建筑的坐标
    private static boolean previewActive = false;
    private static int lastSelectPlansSize = 0;

    // ============================================================
    // A* 寻路：静态复用资源
    // ============================================================
    private static final PQueue<Tile> aStarQueue = new PQueue<>(200 * 200 / 4, (a, b) -> 0);
    private static float[] aStarCosts;
    private static int[] aStarParent;
    private static final Seq<Tile> aStarOut = new Seq<>();

    // ============================================================
    // 路径方向缓存：建造时由路径方向设定的输入方向
    // ============================================================
    private static final java.util.HashMap<Long, Integer> pendingInputDir = new java.util.HashMap<>();

    private static long key(int x, int y) {
        return (long)x << 32 | (y & 0xFFFFFFFFL);
    }

    // ============================================================
    // 辅助结果类型
    // ============================================================
    private static class StartResult {
        final Tile tile;
        StartResult(Tile t) { tile = t; }
    }
    private static class EndResult {
        final Tile tile;
        EndResult(Tile t) { tile = t; }
    }

    // ============================================================
    // 构造
    // ============================================================
    public TransportBelt(String name) {
        super(name);
        speed = 0.008f;
        health = 1024;
        size = 1;
        itemCapacity = 1;
        conveyorPlacement = true;
        hasShadow = false;
        drawArrow = false;
        group = BlockGroup.transportation;
        category = Category.distribution;
        requirements(Category.distribution, ItemStack.with());
    }

    // ============================================================
    // 修正终点格的旋转：始终使用路径方向，而非终点建筑的旋转
    // ============================================================
    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans){
        super.handlePlacementLine(plans);
        if(plans.size >= 2){
            BuildPlan last = plans.peek();
            // 检查最后一格是否邻接可接受物品的建筑
            Tile lastTile = world.tile(last.x, last.y);
            if(lastTile != null){
                for(Point2 p : Geometry.d4){
                    Tile neighbor = world.tile(lastTile.x + p.x, lastTile.y + p.y);
                    if(neighbor != null && neighbor.build != null && canAcceptFrom(neighbor.build, lastTile)){
                        int toBuilding = lastTile.relativeTo(neighbor.x, neighbor.y);
                        if(toBuilding != -1){
                            last.rotation = toBuilding;
                            return;
                        }
                    }
                }
            }
            // 回退：指向路径前一格（延续路径方向）
            BuildPlan secondLast = plans.get(plans.size - 2);
            int correctRot = Tile.relativeTo(secondLast.x, secondLast.y, last.x, last.y);
            if(correctRot != -1){
                last.rotation = correctRot;
            }
        }
    }

    // ============================================================
    // A* 寻路
    // ============================================================
    private static Seq<Tile> pathfind(int sx, int sy, int ex, int ey) {
        Tiles tiles = world.tiles;

        Tile start = tiles.getn(sx, sy);
        Tile end = tiles.getn(ex, ey);
        if (start == null || end == null) return aStarOut.clear();

        int mapSize = tiles.width * tiles.height;
        if (aStarCosts == null || aStarCosts.length != mapSize) {
            aStarCosts = new float[mapSize];
            aStarParent = new int[mapSize];
        }
        java.util.Arrays.fill(aStarCosts, 0);
        java.util.Arrays.fill(aStarParent, -1);

        aStarQueue.clear();
        aStarQueue.comparator = Structs.comparingFloat(
            a -> aStarCosts[a.array()] + Math.abs(a.x - ex) + Math.abs(a.y - ey));
        aStarQueue.add(start);
        GridBits closed = new GridBits(tiles.width, tiles.height);
        closed.set(start.x, start.y);

        boolean found = false;
        while (!aStarQueue.empty()) {
            Tile next = aStarQueue.poll();
            float baseCost = aStarCosts[next.array()];
            if (next == end) {
                found = true;
                break;
            }
            for (Point2 p : Geometry.d4) {
                int nx = next.x + p.x;
                int ny = next.y + p.y;
                if (!Structs.inBounds(nx, ny, tiles.width, tiles.height)) continue;
                Tile child = tiles.getn(nx, ny);
                if (child == null) continue;

                boolean passable;
                if (child == start || child == end) {
                    passable = true;
                } else if (child.build instanceof TransportBeltBuild belt) {
                    // 弯道传送带（输入方向与输出方向不成直线）视为不可通过
                    passable = (belt.inputDir + 2) % 4 == belt.rotation;
                } else {
                    passable = child.build == null && !child.floor().isDeep();
                }

                if (passable && !closed.get(child.x, child.y)) {
                    closed.set(child.x, child.y);
                    float moveCost = baseCost + 1f;
                    // 弯道惩罚
                    int parentIdx = aStarParent[next.array()];
                    if (parentIdx >= 0) {
                        int prevDir = next.relativeTo(parentIdx % tiles.width, parentIdx / tiles.width);
                        int curDir = child.relativeTo(next.x, next.y);
                        if (prevDir != curDir) moveCost += 2f;
                    }
                    aStarCosts[child.array()] = moveCost;
                    aStarParent[child.array()] = next.array();
                    aStarQueue.add(child);
                }
            }
        }

        aStarOut.clear();
        if (!found) return aStarOut;

        // 回溯路径（使用 parent 方向追踪，避免成本贪心死循环）
        Tile current = end;
        int maxSteps = tiles.width * tiles.height; // 安全上限
        int steps = 0;
        while (current != start) {
            aStarOut.add(current);
            int parentIdx = aStarParent[current.array()];
            if (parentIdx < 0 || steps > maxSteps) {
                aStarOut.clear();
                return aStarOut;
            }
            current = tiles.getn(parentIdx % tiles.width, parentIdx / tiles.width);
            steps++;
        }
        aStarOut.add(start);
        aStarOut.reverse();
        return aStarOut;
    }

    // ============================================================
    // 计算矩形工厂的边界
    // ============================================================
    private static void rectBounds(RectGenericAICBasicFacility block, Building build,
                                   int[] out) {
        boolean rotated = build.rotation % 2 != 0;
        int w = rotated ? block.rectHeight : block.rectWidth;
        int h = rotated ? block.rectWidth : block.rectHeight;
        out[0] = build.tileX() - (w % 2 == 0 ? w / 2 - 1 : w / 2);
        out[1] = build.tileX() + (w % 2 == 0 ? w / 2 : w / 2);
        out[2] = build.tileY() - (h % 2 == 0 ? h / 2 - 1 : h / 2);
        out[3] = build.tileY() + (h % 2 == 0 ? h / 2 : h / 2);
    }

    // ============================================================
    // canOutputTo: 建筑 src 能否向 beltTile 输出物品
    // ============================================================
    private static boolean canOutputTo(Building src, Tile beltTile) {
        if (src == null || beltTile == null) return false;

        // RectChildBuild -> 转发到 master
        if (src instanceof RectGenericAICBasicFacility.RectChildBlock.RectChildBuild) {
            Building master = ((RectGenericAICBasicFacility.RectChildBlock.RectChildBuild) src).master;
            return master != null && canOutputTo(master, beltTile);
        }

        // RectBuild: 检查 beltTile 是否在建筑输出排上
        if (src instanceof RectGenericAICBasicFacility.RectBuild) {
            RectGenericAICBasicFacility b = (RectGenericAICBasicFacility) src.block;
            int[] bounds = new int[4];
            rectBounds(b, src, bounds);
            int rot = src.rotation;
            int checkX = 0, checkY = 0;
            switch (rot) {
                case 0: checkX = bounds[1] + 1; break;
                case 1: checkY = bounds[3] + 1; break;
                case 2: checkX = bounds[0] - 1; break;
                case 3: checkY = bounds[2] - 1; break;
            }
            if (rot % 2 == 0) {
                return beltTile.x == checkX && beltTile.y >= bounds[2] && beltTile.y <= bounds[3];
            } else {
                return beltTile.y == checkY && beltTile.x >= bounds[0] && beltTile.x <= bounds[1];
            }
        }

        // 通用方向检查：beltTile 在 src 的输出方向上
        if (src instanceof GenericAICBasicFacility.GenericAICBasicFacilityBuild
            || src instanceof TransportBeltBuild || src.block.outputsItems()) {
            int dx = beltTile.x - src.tile.x;
            int dy = beltTile.y - src.tile.y;
            int expectedRot = -1;
            if (dx > 0) expectedRot = 0;  // 右
            else if (dx < 0) expectedRot = 2;  // 左
            else if (dy > 0) expectedRot = 1;  // 下
            else if (dy < 0) expectedRot = 3;  // 上
            if (expectedRot < 0) return false;

            // 特定类型的额外方向检查
            if (src instanceof TransportBeltBuild) {
                return expectedRot == ((TransportBeltBuild) src).rotation;
            }
            return expectedRot == src.rotation;
        }

        return false;
    }

    // ============================================================
    // canAcceptFrom: beltTile 处的传送带能否向建筑 tgt 输出
    // ============================================================
    private static boolean canAcceptFrom(Building tgt, Tile beltTile) {
        if (tgt == null || beltTile == null) return false;

        if (tgt instanceof RectGenericAICBasicFacility.RectChildBlock.RectChildBuild) {
            Building master = ((RectGenericAICBasicFacility.RectChildBlock.RectChildBuild) tgt).master;
            return master != null && canAcceptFrom(master, beltTile);
        }

        if (tgt instanceof RectGenericAICBasicFacility.RectBuild) {
            RectGenericAICBasicFacility b = (RectGenericAICBasicFacility) tgt.block;
            int[] bounds = new int[4];
            rectBounds(b, tgt, bounds);
            int inDir = (tgt.rotation + 2) % 4;
            int checkX = 0, checkY = 0;
            switch (inDir) {
                case 0: checkX = bounds[1] + 1; break;
                case 1: checkY = bounds[3] + 1; break;
                case 2: checkX = bounds[0] - 1; break;
                case 3: checkY = bounds[2] - 1; break;
            }
            if (inDir % 2 == 0) {
                return beltTile.x == checkX && beltTile.y >= bounds[2] && beltTile.y <= bounds[3];
            } else {
                return beltTile.y == checkY && beltTile.x >= bounds[0] && beltTile.x <= bounds[1];
            }
        }

        // 通用方向检查：beltTile 在 tgt 的输入方向上
        if (tgt instanceof GenericAICBasicFacility.GenericAICBasicFacilityBuild
            || tgt instanceof TransportBeltBuild || (tgt.block.hasItems && tgt.block.acceptsItems)) {
            int dx = beltTile.x - tgt.tile.x;
            int dy = beltTile.y - tgt.tile.y;
            int expectedDir = -1;
            if (dx > 0) expectedDir = 0;
            else if (dx < 0) expectedDir = 2;
            else if (dy > 0) expectedDir = 1;
            else if (dy < 0) expectedDir = 3;
            if (expectedDir < 0) return false;

            if (tgt instanceof TransportBeltBuild) {
                return expectedDir == ((TransportBeltBuild) tgt).inputDir;
            }
            // 建筑的输入方向是旋转的反方向
            return expectedDir == (tgt.rotation + 2) % 4;
        }

        return false;
    }

    // ============================================================
    // findEffectiveStart / findEffectiveEnd
    // ============================================================
    private static StartResult findEffectiveStart(Tile clicked) {
        if (clicked == null) return null;

        if (clicked.build != null) {
            for (Point2 p : Geometry.d4) {
                Tile neighbor = world.tile(clicked.x + p.x, clicked.y + p.y);
                if (neighbor == null || neighbor.build != null) continue;
                if (canOutputTo(clicked.build, neighbor)) {
                    return new StartResult(neighbor);
                }
            }
        } else {
            for (Point2 p : Geometry.d4) {
                Tile neighbor = world.tile(clicked.x + p.x, clicked.y + p.y);
                if (neighbor == null || neighbor.build == null) continue;
                if (canOutputTo(neighbor.build, clicked)) {
                    return new StartResult(clicked);
                }
            }
        }
        return null;
    }

    private static EndResult findEffectiveEnd(Tile clicked) {
        if (clicked == null) return null;

        if (clicked.build != null) {
            for (Point2 p : Geometry.d4) {
                Tile neighbor = world.tile(clicked.x + p.x, clicked.y + p.y);
                if (neighbor == null || neighbor.build != null) continue;
                if (canAcceptFrom(clicked.build, neighbor)) {
                    return new EndResult(neighbor);
                }
            }
        } else {
            return new EndResult(clicked);
        }
        return null;
    }

    // 查找可以向指定 beltTile 输出物品的源建筑
    private static Tile findSourceBuilding(Tile beltTile) {
        if (beltTile == null) return null;
        for (Point2 p : Geometry.d4) {
            Tile neighbor = world.tile(beltTile.x + p.x, beltTile.y + p.y);
            if (neighbor != null && neighbor.build != null && canOutputTo(neighbor.build, beltTile)) {
                return neighbor;
            }
        }
        return null;
    }

    // ============================================================
    // changePlacementPath: Desktop 状态机 + 路径替换
    // ============================================================
    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        if (points.isEmpty()) return;

        // 拦截右键取消配置模式：changePlacementPath 在输入处理阶段运行，
        // 在此处处理可以阻止游戏引擎随后取消建筑选择
        if (Core.input.keyTap(Binding.deselect)) {
            if (configuring) {
                configuring = false;
                previewActive = false;
                configStartX = -1;
                configStartY = -1;
                configSourceX = -1;
                configSourceY = -1;
            }
            points.clear();
            Vars.control.input.block = this;
            return;
        }

        Point2 first = points.first();
        Point2 last = points.peek();
        boolean keyTap = Core.input.keyTap(Binding.select);
        boolean keyDown = Core.input.keyDown(Binding.select);
        boolean validFirst = world.tile(first.x, first.y) != null;

        if (!configuring && keyDown) {
            // 首次左键按下时启动配置
            Tile ft = validFirst ? world.tile(first.x, first.y) : null;
            if (ft != null) {
                StartResult s = findEffectiveStart(ft);
                if (s != null) {
                    configStartX = s.tile.x;
                    configStartY = s.tile.y;
                    // 记录起点建筑的坐标，用于后续计算首节传送带的输入方向
                    Tile source = findSourceBuilding(s.tile);
                    configSourceX = source != null ? source.x : -1;
                    configSourceY = source != null ? source.y : -1;
                    configuring = true;
                    previewActive = true;
                }
            }
            points.clear();
            // 强制清空 linePlans 避免意外残留
            if (Vars.control.input.linePlans.size > 0) {
                Vars.control.input.linePlans.clear();
            }
            return;
        }

        if (keyTap && validFirst) {
            Tile lt = world.tile(last.x, last.y);
            if (lt != null) {
                EndResult e = findEffectiveEnd(lt);
                if (e != null) {
                    Seq<Tile> path = pathfind(configStartX, configStartY, e.tile.x, e.tile.y);
                    if (!path.isEmpty()) {
                        points.clear();
                        for (int pi = 0; pi < path.size; pi++) {
                            Tile t = path.get(pi);
                            int inDir;
                            if (pi == 0) {
                                // 首节传送带：输入方向 = 从传送带到源建筑
                                Tile source = world.tile(configSourceX, configSourceY);
                                inDir = source != null ? t.relativeTo(source.x, source.y) : 0;
                            } else {
                                inDir = t.relativeTo(path.get(pi - 1).x, path.get(pi - 1).y);
                            }
                            pendingInputDir.put(key(t.x, t.y), inDir);
                        }
                        for (Tile t : path) {
                            points.add(new Point2(t.x, t.y));
                        }
                        configuring = false;
                        previewActive = false;
                        return;
                    }
                }
            }
            points.clear();
            return;
        }

        if (!keyDown) {
            Tile lt = world.tile(last.x, last.y);
            EndResult ee = lt != null ? findEffectiveEnd(lt) : null;
            int px = ee != null ? ee.tile.x : last.x;
            int py = ee != null ? ee.tile.y : last.y;
            Seq<Tile> preview = pathfind(configStartX, configStartY, px, py);
            points.clear();
            if (!preview.isEmpty()) {
                for (Tile t : preview) {
                    points.add(new Point2(t.x, t.y));
                }
            }
            previewActive = true;
            return;
        }

        points.clear();
    }

    // ============================================================
    // drawPlace: Mobile 点击检测 + 通用绘制
    // ============================================================
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        // 仅绘制一个方框表示指针所在格子，不绘制传送带投影
        Drawf.square(x * tilesize + offset, y * tilesize + offset,
            tilesize / 2f + 2f, valid ? Pal.accent : Pal.remove);

        Tile hover = world.tile(x, y);
        if (hover == null) return;

        // Mobile 点击检测
        int curSize = Vars.control.input.selectPlans.size;
        if (curSize > lastSelectPlansSize) {
            lastSelectPlansSize = curSize;
            if (curSize > 0) {
                Vars.control.input.selectPlans.remove(curSize - 1);
            }

            if (!configuring) {
                StartResult s = findEffectiveStart(hover);
                if (s != null) {
                    configStartX = s.tile.x;
                    configStartY = s.tile.y;
                    // 记录起点建筑的坐标
                    Tile source = findSourceBuilding(s.tile);
                    configSourceX = source != null ? source.x : -1;
                    configSourceY = source != null ? source.y : -1;
                    configuring = true;
                    previewActive = true;
                }
            } else {
                EndResult e = findEffectiveEnd(hover);
                if (e != null) {
                    Seq<Tile> path = pathfind(configStartX, configStartY, e.tile.x, e.tile.y);
                    if (!path.isEmpty()) {
                        for (int i = 0; i < path.size; i++) {
                            Tile t = path.get(i);
                            int rot;
                            if (i < path.size - 1) {
                                // relativeTo 返回方向编码与旋转一致
                                int rel = t.relativeTo(path.get(i + 1).x, path.get(i + 1).y);
                                rot = rel;
                            } else {
                                // 最后一格：若 hover 上有建筑则朝向建筑，否则延续路径方向
                                int rel;
                                if(hover.build != null){
                                    rel = t.relativeTo(hover.x, hover.y);
                                } else {
                                    rel = path.get(i - 1).relativeTo(t.x, t.y);
                                }
                                rot = rel;
                            }
                            // 根据路径方向设定输入方向
                            int inDir;
                            if (i == 0) {
                                // 首节传送带：输入方向 = 从传送带到源建筑
                                Tile source = world.tile(configSourceX, configSourceY);
                                inDir = source != null ? t.relativeTo(source.x, source.y) : 0;
                            } else {
                                inDir = t.relativeTo(path.get(i - 1).x, path.get(i - 1).y);
                            }
                            pendingInputDir.put(key(t.x, t.y), inDir);
                            Vars.control.input.selectPlans.add(
                                new BuildPlan(t.x, t.y, rot, this, null));
                        }
                        configuring = false;
                        previewActive = false;
                        return;
                    }
                }
            }
        } else {
            lastSelectPlansSize = curSize;
        }

        // 强制清除 linePlans（配置模式中每帧清除）
        if (Vars.control.input.linePlans.size > 0) {
            Vars.control.input.linePlans.clear();
        }

        // 通用绘制
        if (configuring && previewActive) {
            Drawf.square(
                configStartX * tilesize + offset,
                configStartY * tilesize + offset,
                tilesize / 2f + 2f, Pal.accent);

            EndResult e = findEffectiveEnd(hover);
            if (e != null) {
                Drawf.square(e.tile.drawx(), e.tile.drawy(),
                    tilesize / 2f + 2f, Pal.place);
            }

            // 预览终点应对齐有效终点（若有），与实际建造一致
            int px = e != null ? e.tile.x : x;
            int py = e != null ? e.tile.y : y;
            Seq<Tile> preview = pathfind(configStartX, configStartY, px, py);
            if (!preview.isEmpty()) {
                Draw.color(Pal.accent);
                Draw.alpha(0.35f);
                for (int pi = 0; pi < preview.size; pi++) {
                    Tile t = preview.get(pi);
                    int ridx = previewBlendRegion(preview, pi);
                    float prot = previewRotation(preview, pi);
                    TextureRegion pr = ridx >= 0 && ridx < regions.length ? regions[ridx][0] : region;
                    float pw = pr.width * pr.scl();
                    float ph = pr.height * pr.scl();
                    // 弯道处理：顺时针弯道需垂直翻转
                    if (ridx == 1 && pi > 0 && pi < preview.size - 1) {
                        int inDir = t.relativeTo(preview.get(pi - 1).x, preview.get(pi - 1).y);
                        int outDir = t.relativeTo(preview.get(pi + 1).x, preview.get(pi + 1).y);
                        if (((outDir - inDir + 4) % 4) == 1) { // 顺时针弯道
                            ph = -ph;
                        }
                    }
                    Draw.rect(pr, t.worldx(), t.worldy(), pw, ph, prot);
                }
                Draw.reset();
            }
        } else if (!configuring) {
            StartResult s = findEffectiveStart(hover);
            if (s != null) {
                Drawf.square(s.tile.drawx(), s.tile.drawy(),
                    tilesize / 2f + 2f, Pal.accent);
            } else {
                // 无效起点提示
                drawPlaceText("无效起点", x, y, false);
            }
        }
        
        // 终点无效提示
        if (configuring && previewActive && findEffectiveEnd(hover) == null) {
            drawPlaceText("无效终点", x, y, false);
        }

        // drawPlace 中右键取消作为兜底
        if (configuring && Core.input.keyTap(Binding.deselect)) {
            configuring = false;
            previewActive = false;
            configStartX = -1;
            configStartY = -1;
            configSourceX = -1;
            configSourceY = -1;
            Vars.control.input.block = this;
        }
    }

    // ============================================================
    // 预览纹理辅助
    // ============================================================
    private static int previewBlendRegion(Seq<Tile> path, int idx) {
        Tile me = path.get(idx);
        int connMask = 0;
        for (int d = 0; d < 4; d++) {
            int nx = me.x + Geometry.d4x(d);
            int ny = me.y + Geometry.d4y(d);
            for (int j = 0; j < path.size; j++) {
                if (j == idx) continue;
                Tile o = path.get(j);
                if (o.x == nx && o.y == ny) { connMask |= (1 << d); break; }
            }
        }
        int bits = Integer.bitCount(connMask);
        if (bits <= 1) return 5;  // 端头
        // 检查是否直线 (相对方向: 0↔2, 1↔3)
        for (int d = 0; d < 4; d++) {
            if ((connMask & (1 << d)) != 0 && (connMask & (1 << ((d + 2) % 4))) != 0) {
                return 0; // 直线段
            }
        }
        return 1; // 弯道
    }

    private static float previewRotation(Seq<Tile> path, int idx) {
        Tile me = path.get(idx);
        Tile next = idx < path.size - 1 ? path.get(idx + 1) : (idx > 0 ? path.get(idx - 1) : null);
        if (next == null) return 0;
        int dx = next.x - me.x, dy = next.y - me.y;
        // Draw.rect 使用顺时针旋转，底座有朝右
        if (dx > 0) return 0f;    // 右 0°
        if (dx < 0) return 180f;  // 左 180°
        if (dy > 0) return 90f;   // 下 270°
        return 270f;             // 上 90°
    }

    // ============================================================
    // drawPlan: 预览路径半透明渲染
    // ============================================================
    @Override
    public void drawPlan(BuildPlan plan, Eachable<BuildPlan> list, boolean valid, float alpha) {
        if (previewActive && configuring) {
            // 配置模式：半透明绘制预览路径
            super.drawPlan(plan, list, valid, 0.35f);
        } else {
            // 非配置模式：由 drawPlace 处理光标处的方框渲染
            // drawPlan 不绘制任何内容，避免覆盖 drawPlace 的视觉反馈
        }
    }

    // ============================================================
    // TransportBeltBuild
    // ============================================================
    public class TransportBeltBuild extends ConveyorBuild {
        private static final float itemSpace = 0.4f;
        public int inputDir;

        @Override
        public void created() {
            super.created();
            long k = key(tileX(), tileY());
            Integer dir = pendingInputDir.remove(k);
            // 输入方向严格由路径计算缓存决定，永不从 rotation 推导
            inputDir = dir != null ? dir : 0;
        }

        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            // 保留输入和输出方向侧的混合连接，清除其他方向（防止三叉/十字纹理）
            // 必须使用 inputDir 而非 (rotation+2)%4，因为弯道时两者不同！
            blending &= (1 << inputDir) | (1 << rotation);
            // 根据实际的 inputDir/rotation 关系正确设置 blendbits
            // 直道: inputDir 与 rotation 相反 → blendbits=0
            // 弯道: inputDir 与 rotation 相邻 → blendbits=1
            blendbits = ((inputDir + 2) % 4 == rotation) ? 0 : 1;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (source == null) return false;

            if (source instanceof RectGenericAICBasicFacility.RectBuild) {
                if (source.rotation != ((inputDir + 2) % 4)) return false;
                int checkX = this.tileX();
                int checkY = this.tileY();
                switch(this.inputDir) {
                    case 0: checkX = checkX + 1; break;
                    case 1: checkY = checkY + 1; break;
                    case 2: checkX = checkX - 1; break;
                    case 3: checkY = checkY - 1; break;
                }
                if (Vars.world.tile(checkX, checkY) == null || Vars.world.tile(checkX, checkY).build == null) {
                    return false;
                } else {
                    if (Vars.world.tile(checkX, checkY).build.block instanceof RectGenericAICBasicFacility.RectChildBlock) {
                        if (source != ((RectGenericAICBasicFacility.RectChildBlock.RectChildBuild) Vars.world.tile(checkX, checkY).build).master) {
                            return false;
                        }
                    }
                }
            } else {
                Tile facing = Edges.getFacingEdge(source.tile, tile);
                if (facing == null) return false;
                int sourceDir = (facing.relativeTo(tile.x, tile.y) + 2) % 4;
                if (sourceDir != inputDir) return false;
            }

            if (len != 0) return false;
            return true;
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source) {
            if (source instanceof Building) {
                Building src = (Building) source;
                if (tile.relativeTo(src.tile) != inputDir) return 0;
            }
            return len == 0 ? Math.min(1, amount) : 0;
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source) {
            amount = Math.min(amount, 1);
            if (amount > 0 && len == 0) {
                add(0);
                xs[0] = 0;
                ys[0] = 0;
                ids[0] = item;
                items.add(item, 1);
                noSleep();
            }
        }

        @Override
        public void handleItem(Building source, Item item) {
            if (len != 0) return;

            int r = rotation;
            Tile facing = Edges.getFacingEdge(source.tile, tile);
            int ang = ((facing.relativeTo(tile.x, tile.y) - r));
            float x = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;

            noSleep();
            items.add(item, 1);

            if (Math.abs(facing.relativeTo(tile.x, tile.y) - r) == 0) {
                add(0);
                xs[0] = x;
                ys[0] = 0;
                ids[0] = item;
            } else {
                add(0);
                xs[0] = x;
                ys[0] = 0.5f;
                ids[0] = item;
            }
        }

        @Override
        public void updateTile() {
            if (len == 0) {
                clogHeat = 0f;
                sleep();
                return;
            }

            float moved = speed * edelta();
            float nextMax = 1f;
            if (aligned && nextc != null) {
                nextMax = 1f - Math.max(itemSpace - nextc.minitem, 0);
            }

            ys[0] += moved;
            if (ys[0] > nextMax) ys[0] = nextMax;
            xs[0] = Mathf.approach(xs[0], 0, moved * 2);

            if (ys[0] >= 1f && pass(ids[0])) {
                items.remove(ids[0], 1);
                remove(0);
            }

            minitem = len > 0 ? ys[0] : 1f;

            if (len > 0 && minitem < itemSpace) {
                clogHeat = Mathf.approachDelta(clogHeat, 1f, 1f / 60f);
            } else {
                clogHeat = 0f;
            }

            noSleep();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(inputDir);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            inputDir = read.i();
        }
    }
}