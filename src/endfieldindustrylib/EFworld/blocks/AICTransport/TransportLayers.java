package endfieldindustrylib.EFworld.blocks.AICTransport;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.PixmapRegion;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.util.Time;
import mindustry.Vars;
import static mindustry.Vars.tilesize;
import mindustry.entities.Effect;
import mindustry.graphics.Drawf;
import mindustry.graphics.MultiPacker;

/**
 * AIC 传输方块统一分层贴图与选择发光辅助。
 *
 * 贴图构成：base → base-cell → 本体(body) → cell，四层顺序绘制，缺失（null）视为没有对应层。
 * -select 为选择发光层：仅在指针指向方块 / 放置时通过 drawOverlay 显示在顶部并发光。
 */
public final class TransportLayers {

    private TransportLayers(){}

    /** 查找可选贴图（依次尝试多个名字，缺失返回 null，视为没有该层）。
     *  模组精灵在 atlas 中带 mod 名前缀（如 endfield-industry-lib-transporting），故需同时尝试原名与前缀名。 */
    public static TextureRegion opt(String... names){
        for(String n : names){
            if(Core.atlas.has(n)) return Core.atlas.find(n);
        }
        return null;
    }

    /** 按 base → base-cell → 本体 → cell 顺序绘制四层（null 跳过）；cell 层为队伍色投影。 */
    public static void draw(TextureRegion base, TextureRegion baseCell, TextureRegion body, TextureRegion cell, float x, float y, float rot, Color teamColor){
        if(base != null) Draw.rect(base, x, y, rot);
        if(baseCell != null) drawCell(baseCell, x, y, rot, teamColor);
        Draw.rect(body, x, y, rot);
        if(cell != null) drawCell(cell, x, y, rot, teamColor);
    }

    /** cell 投影层：队伍颜色 + additive 混合（发光投影）。 */
    private static void drawCell(TextureRegion region, float x, float y, float rot, Color teamColor){
        Draw.blend(Blending.additive);
        Draw.color(teamColor);
        Draw.rect(region, x, y, rot);
        Draw.blend();
        Draw.color();
    }

    /** 放置预览使用的队伍颜色（本地玩家队伍；无玩家时为白色）。 */
    public static Color planTeamColor(){
        return Vars.player != null && Vars.player.team() != null ? Vars.player.team().color : Color.white;
    }

    /**
     * 建造列表图标：将各层合成为一张与实际建筑一致的图标（RGBA8888）。
     * 顺序 base → base-cell → 本体 → cell（source-over），select 层加色叠加（发光）。
     * 以 `name + "-full"` 命名，二次 loadIcon() 会将其作为 fullIcon/uiIcon。
     */
    public static void createCompositeIcon(MultiPacker packer, String iconName, TextureRegion base, TextureRegion baseCell, TextureRegion body, TextureRegion cell, TextureRegion select){
        if(body == null || !body.found()) return;
        int w = body.width, h = body.height;
        if(w <= 0 || h <= 0) return;

        Pixmap out = new Pixmap(w, h);
        PixmapRegion baseR = base != null && base.found() ? packer.get(base) : null;
        PixmapRegion baseCellR = baseCell != null && baseCell.found() ? packer.get(baseCell) : null;
        PixmapRegion bodyR = packer.get(body);
        PixmapRegion cellR = cell != null && cell.found() ? packer.get(cell) : null;
        PixmapRegion selectR = select != null && select.found() ? packer.get(select) : null;

        int[] acc = new int[4];
        for(int y = 0; y < h; y++){
            for(int x = 0; x < w; x++){
                acc[0] = acc[1] = acc[2] = acc[3] = 0;
                if(baseR != null) blendOver(acc, getSafe(baseR, x, y));
                if(baseCellR != null) blendOver(acc, getSafe(baseCellR, x, y));
                blendOver(acc, getSafe(bodyR, x, y));
                if(cellR != null) blendOver(acc, getSafe(cellR, x, y));
                if(selectR != null) blendAdd(acc, getSafe(selectR, x, y));
                out.set(x, y, pack(acc));
            }
        }

        packer.add(MultiPacker.PageType.main, iconName, out);
        out.dispose();
    }

    private static int getSafe(PixmapRegion r, int x, int y){
        if(x < 0 || y < 0 || x >= r.width || y >= r.height) return 0;
        return r.get(x, y);
    }

    /** source-over 混合（RGBA8888）。 */
    private static void blendOver(int[] acc, int src){
        int sa = src & 0xFF;
        if(sa == 0) return;
        int sr = (src >>> 24) & 0xFF, sg = (src >>> 16) & 0xFF, sb = (src >>> 8) & 0xFF;
        int da = acc[3];
        int outA = sa + da * (255 - sa) / 255;
        if(outA == 0){
            acc[0] = sr; acc[1] = sg; acc[2] = sb; acc[3] = sa;
            return;
        }
        acc[0] = (sr * sa + acc[0] * da * (255 - sa) / 255) / outA;
        acc[1] = (sg * sa + acc[1] * da * (255 - sa) / 255) / outA;
        acc[2] = (sb * sa + acc[2] * da * (255 - sa) / 255) / outA;
        acc[3] = outA;
    }

    /** 加色叠加（用于 select 发光层）。 */
    private static void blendAdd(int[] acc, int src){
        int sa = src & 0xFF;
        if(sa == 0) return;
        int sr = (src >>> 24) & 0xFF, sg = (src >>> 16) & 0xFF, sb = (src >>> 8) & 0xFF;
        acc[0] = Math.min(255, acc[0] + sr * sa / 255);
        acc[1] = Math.min(255, acc[1] + sg * sa / 255);
        acc[2] = Math.min(255, acc[2] + sb * sa / 255);
        acc[3] = Math.max(acc[3], sa);
    }

    private static int pack(int[] acc){
        return (acc[0] << 24) | (acc[1] << 16) | (acc[2] << 8) | acc[3];
    }

    /** 选择发光层：additive 混合（保留贴图本色）+ 呼吸脉冲 + 辉光。 */
    public static void select(TextureRegion select, float x, float y, float rot){
        if(select == null) return;
        float pulse = 0.5f + 0.5f * Mathf.absin(Time.time, 5f, 1f);
        Draw.blend(Blending.additive);
        Draw.color(1f, 1f, 1f, 0.5f + 0.5f * pulse);
        Draw.rect(select, x, y, rot);
        Drawf.light(x, y, select, rot, Color.white, 0.5f * pulse);
        Draw.blend();
    }

    // ============================================================
    // 物品流动箭头特效（transporting.png，朝上箭头）
    // acceptItem 成功 → 输入侧边缘→方块中心；handleItem → 中心→输出侧边缘
    // 箭头发光（additive + 光晕 + 辉光）并带滑动/淡入淡出/脉冲动态
    // ============================================================
    public static final float ARROW_COOLDOWN = 15f;

    private static TextureRegion arrowRegion;
    private static Effect arrowIn, arrowOut;

    static {
        // 输入箭头：输入侧边缘 → 方块中心（加速滑入）
        // e.data 传入方向 d（0右/1下/2左/3上，与 Geometry.d4 一致）
        arrowIn = new Effect(16f, 20f, e -> {
            int d = (int)(float)(Float)e.data();
            float dx = Geometry.d4x(d), dy = Geometry.d4y(d);
            float f = e.fin(Interp.pow2In);
            float px = Mathf.lerp(e.x + dx * tilesize / 2f, e.x, f);
            float py = Mathf.lerp(e.y + dy * tilesize / 2f, e.y, f);
            drawArrow(px, py, -d, e.color, e.fslope());
        });

        // 输出箭头：方块中心 → 输出侧边缘（减速滑出）
        arrowOut = new Effect(16f, 20f, e -> {
            int d = (int)(float)(Float)e.data();
            float dx = Geometry.d4x(d), dy = Geometry.d4y(d);
            float f = e.fin(Interp.pow2Out);
            float px = Mathf.lerp(e.x, e.x + dx * tilesize / 2f, f);
            float py = Mathf.lerp(e.y, e.y + dy * tilesize / 2f, f);
            drawArrow(px, py, d, e.color, e.fslope());
        });
    }

    /** 加载箭头贴图（transporting.png，带 mod 名前缀回退）；方块 load() 时调用。 */
    public static void loadArrows(String pfx){
        arrowRegion = opt("transporting", pfx + "transporting");
    }

    /** 生成输入箭头（输入侧边缘→中心）。 */
    public static void spawnArrowIn(float x, float y, int dir, Color color){
        if(arrowRegion == null || !arrowRegion.found()) return;
        arrowIn.at(x, y, 0f, color, (float)dir);
    }

    /** 生成输出箭头（中心→输出侧边缘）。 */
    public static void spawnArrowOut(float x, float y, int dir, Color color){
        if(arrowRegion == null || !arrowRegion.found()) return;
        arrowOut.at(x, y, 0f, color, (float)dir);
    }

    // 发光箭头绘制：additive 混合 + 双层光晕 + 尺寸脉冲 + 真实辉光
    private static void drawArrow(float x, float y, int dir, Color color, float alpha){
        float rot = dir * 90f; // transporting 朝上=270°，Draw.rect 顺时针：0右/90下/180左/270上
        float pulse = 1f + 0.12f * Mathf.sin(Time.time * 0.35f + (x + y));
        float arrowSize = 6f * pulse;

        Draw.blend(Blending.additive);
        Draw.color(color, alpha * 0.35f);
        Draw.rect(arrowRegion, x, y, arrowSize * 1.7f, arrowSize * 1.7f, rot);   // 外层光晕
        Draw.color(color, alpha);
        Draw.rect(arrowRegion, x, y, arrowSize, arrowSize, rot);                 // 明亮内核
        Drawf.light(x, y, arrowRegion, rot, color, 0.5f * alpha);
        Draw.blend();
    }
}
