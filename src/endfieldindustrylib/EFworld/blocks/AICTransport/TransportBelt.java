package endfieldindustrylib.EFworld.blocks.AICTransport;

import arc.Core;
import arc.Events;
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
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.GenericAICBasicFacility;
import endfieldindustrylib.EFworld.blocks.AICBasicFacility.RectGenericAICBasicFacility;
import mindustry.Vars;
import static mindustry.Vars.itemSize;
import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Teamc;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.distribution.Duct;
import mindustry.world.blocks.distribution.StackConveyor;
import mindustry.world.blocks.environment.StaticWall;
import mindustry.world.blocks.environment.StaticTree;
import mindustry.world.blocks.defense.Wall;
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

    private long key(int x, int y) {
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
    // 8 帧动画：重载 regions 为 7 种混合类型 × 8 帧 + 独立循环关闭检测
    // ============================================================
    private static boolean updateRegistered = false;

    @Override
    public void load(){
        super.load();
        // 父类按 4 帧加载，这里改为 8 帧（命名与父类一致：name-混合-帧，0 起始）
        regions = new TextureRegion[7][8];
        for(int i = 0; i < regions.length; i++){
            for(int j = 0; j < regions[i].length; j++){
                regions[i][j] = Core.atlas.find(name + "-" + i + "-" + j);
            }
        }

        // 独立循环检测：每帧检查当前选择建造的方块，若不再是本传送带则关闭配置模式
        if(!updateRegistered){
            updateRegistered = true;
            Events.run(Trigger.update, () -> {
                // 只要当前选择建造的不是本传送带就关闭配置模式
                if(Vars.control.input.block != this){
                    closeConfig();
                }
            });
        }
    }

    // ============================================================
    // blends 适配：仅允许在设施输出面连接
    // ============================================================
    /*@Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        // 若邻接方块是 AIC 设施，仅当传送带位于设施输出面时才混合
        if (otherblock instanceof GenericAICBasicFacility) {
            Building otherBuild = world.tile(otherx, othery) != null ? world.tile(otherx, othery).build : null;
            if (otherBuild != null) {
                int dirToConveyor = Tile.relativeTo(otherx, othery, tile.x, tile.y);
                return GenericAICBasicFacility.isOutputFace(otherBuild, dirToConveyor);
            }
            return false;
        }
        return (otherblock.outputsItems() || (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems))
            && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
    }*/

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
    private Seq<Tile> pathfind(int sx, int sy, int ex, int ey) {
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
                    passable = child.build == null
                        && !(child.block() instanceof StaticWall)
                        && !(child.block() instanceof Wall)
                        && !(child.block() instanceof StaticTree)
                        && child.floor() != null && !child.floor().isDeep();
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


    private static Building resolveMaster(Building source) {
        if (source instanceof RectGenericAICBasicFacility.RectChildBlock.RectChildBuild child) {
            return child.master != null ? child.master : source;
        }
        return source;
    }

    private boolean canOutputTo(Building source, Tile target) {
        if (source == null || target == null) return false;
        source = resolveMaster(source);
        if (source.block instanceof TransportBelt && source instanceof TransportBeltBuild belt) {
            // 传送带作为起点：只能从其输出方向（rotation）的相邻格开始
            return target.x == belt.tileX() + Geometry.d4x(belt.rotation)
                && target.y == belt.tileY() + Geometry.d4y(belt.rotation);
        } else if (source.block instanceof GenericAICBasicFacility && source instanceof GenericAICBasicFacility.GenericAICBasicFacilityBuild build) {
            // 使用输出格子列表：目标必须位于某个输出格
            for (Point2 pos : build.canoutputtile) {
                if (build.worldTileFor(pos) == target) return true;
            }
            return false;
        } else if (source.block instanceof Conveyor || source.block instanceof StackConveyor || source.block instanceof Duct) {
            // 原版定向传送带（Conveyor/StackConveyor/Duct 等）：只能从输出方向（rotation）相邻格开始
            if (!source.block.hasItems) return false;
            return target.x == source.tileX() + Geometry.d4x(source.rotation)
                && target.y == source.tileY() + Geometry.d4y(source.rotation);
        } else {
            // 原版方块：检查能否输出物品 + 目标格是否相邻
            if (!source.block.outputsItems() || !source.block.hasItems) return false;
            int half = source.block.size / 2;
            int minX = source.tileX() - (source.block.size % 2 == 0 ? half - 1 : half);
            int maxX = source.tileX() + half;
            int minY = source.tileY() - (source.block.size % 2 == 0 ? half - 1 : half);
            int maxY = source.tileY() + half;
            return (target.x >= minX && target.x <= maxX && (target.y == minY - 1 || target.y == maxY + 1))
                || (target.y >= minY && target.y <= maxY && (target.x == minX - 1 || target.x == maxX + 1));
        }
    }

    private boolean canAcceptFrom(Building source, Tile target) {
        if (source == null || target == null) return false;
        source = resolveMaster(source);
        if (source.block instanceof TransportBelt && source instanceof TransportBeltBuild belt) {
            // 传送带作为终点：只能从其输入方向（inputDir）的相邻格接入
            return target.x == belt.tileX() + Geometry.d4x(belt.inputDir)
                && target.y == belt.tileY() + Geometry.d4y(belt.inputDir);
        } else if (source.block instanceof GenericAICBasicFacility && source instanceof GenericAICBasicFacility.GenericAICBasicFacilityBuild build) {
            // 使用输入格子列表：目标必须位于某个输入格
            for (Point2 pos : build.caninputtile) {
                if (build.worldTileFor(pos) == target) return true;
            }
            return false;
        } else if (source.block instanceof Conveyor || source.block instanceof StackConveyor || source.block instanceof Duct) {
            // 原版定向传送带（Conveyor/StackConveyor/Duct 等）：只能从输入方向（rotation+2，背面）相邻格接入
            if (!source.block.hasItems) return false;
            int inputDir = (source.rotation + 2) % 4;
            return target.x == source.tileX() + Geometry.d4x(inputDir)
                && target.y == source.tileY() + Geometry.d4y(inputDir);
        } else {
            // 原版方块：检查能否接受物品 + 目标格是否相邻
            if (!source.block.hasItems) return false;
            int half = source.block.size / 2;
            int minX = source.tileX() - (source.block.size % 2 == 0 ? half - 1 : half);
            int maxX = source.tileX() + half;
            int minY = source.tileY() - (source.block.size % 2 == 0 ? half - 1 : half);
            int maxY = source.tileY() + half;
            return (target.x >= minX && target.x <= maxX && (target.y == minY - 1 || target.y == maxY + 1))
                || (target.y >= minY && target.y <= maxY && (target.x == minX - 1 || target.x == maxX + 1));
        }
    }
    // ============================================================
    // findEffectiveStart / findEffectiveEnd
    // ============================================================
    private StartResult findEffectiveStart(Tile clicked) {
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

    private EndResult findEffectiveEnd(Tile clicked) {
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
    private Tile findSourceBuilding(Tile beltTile) {
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
    // 配置模式：关闭与取消选择检测
    // ============================================================
    private static void closeConfig() {
        configuring = false;
        previewActive = false;
        configStartX = -1;
        configStartY = -1;
        configSourceX = -1;
        configSourceY = -1;
    }


    // ============================================================
    // changePlacementPath: Desktop 状态机 + 路径替换
    // ============================================================
    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        if (points.isEmpty()) return;

        // 切换方块（右下角选择其他建筑或取消选择）时关闭配置模式
        if (Vars.control.input.block != this) {
            closeConfig();
            points.clear();
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
                        closeConfig();
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
                        closeConfig();
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
            } else if (e != null) {
                // 终点有效但路径不可达
                drawPlaceText("无效终点", x, y, false);
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
    }

    // ============================================================
    // 预览纹理辅助
    // ============================================================
    private int previewBlendRegion(Seq<Tile> path, int idx) {
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

    private float previewRotation(Seq<Tile> path, int idx) {
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
            super.onProximityUpdate(); // 保留 nextc/aligned 更新（物品传输需要）；blending 由实际邻居计算

            // 输入/输出方向完全由路径决定（inputDir + rotation），不依赖 buildBlending 的邻居扫描，避免三叉/十字误判
            int inputRelDir = (rotation - inputDir + 4) % 4; // 输入方向相对输出方向（rotation）的方位

            // 纹理类型与翻转：直道（输入在背面，相对方位2）=0；弯道=1，并按弯向设置垂直翻转
            blendbits = (inputRelDir == 2) ? 0 : 1;
            blendscly = (inputRelDir == 1) ? -1 : 1;
            blendsclx = 1;

            // 延伸段（半截带）只保留在输入/输出方向侧（起/终点连接建筑时自然显示）
            blending &= (1 << 0) | (1 << inputRelDir);
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
        public void draw(){
            // 8 帧动画：帧号取模 8
            int frame = enabled && clogHeat <= 0.5f ? (int)(((Time.time * speed * 8f * timeScale * efficiency)) % 8) : 0;

            // 绘制指向本传送带的额外混合段
            Draw.z(Layer.blockUnder);
            for(int i = 0; i < 4; i++){
                if((blending & (1 << i)) != 0){
                    int dir = rotation - i;
                    float rot = i == 0 ? rotation * 90 : (dir) * 90;

                    Draw.rect(sliced(regions[0][frame], i != 0 ? SliceMode.bottom : SliceMode.top), x + Geometry.d4x(dir) * tilesize * 0.75f, y + Geometry.d4y(dir) * tilesize * 0.75f, rot);
                }
            }

            Draw.z(Layer.block - 0.2f);
            Draw.rect(regions[blendbits][frame], x, y, tilesize * blendsclx, tilesize * blendscly, rotation * 90);

            // 绘制传送带上的物品
            Draw.z(Layer.block - 0.1f);
            float layer = Layer.block - 0.1f, wwidth = world.unitWidth(), wheight = world.unitHeight(), scaling = 0.01f;

            for(int i = 0; i < len; i++){
                Item item = ids[i];
                Tmp.v1.trns(rotation * 90, tilesize, 0);
                Tmp.v2.trns(rotation * 90, -tilesize / 2f, xs[i] * tilesize / 2f);

                float
                ix = (x + Tmp.v1.x * ys[i] + Tmp.v2.x),
                iy = (y + Tmp.v1.y * ys[i] + Tmp.v2.y);

                Draw.z(layer + (ix / wwidth + iy / wheight) * scaling);
                Draw.rect(item.fullIcon, ix, iy, itemSize, itemSize);
            }
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