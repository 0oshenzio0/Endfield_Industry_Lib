package endfieldindustrylib.EFworld.blocks.AICErosion;

import arc.util.Time;
import endfieldindustrylib.EFcontents.EFunits;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

/**
 * 侵蚀核 — 战役目标方块。
 * <p>
 * 1×1，免疫所有武器/爆炸伤害，禁止右键拆除。
 * <b>只有单位"塔塔"（{@link EFunits#tata}）靠近侵蚀核约 2 秒后核才会崩解。</b>
 * 相邻的 {@link ErosionWall 侵蚀墙体} 依赖本方块维持结构稳定。
 */
public class ErosionCore extends Block {

    /** 塔塔贴近方块后摧毁所需时长（tick） */
    private static final float TATA_DESTROY_TIME = 120f; // ~2 秒 @ 60 tick/s

    public ErosionCore(String name) {
        super(name);
        // 基础属性
        update = true;          // 需要每帧检测附近是否有塔塔
        solid = true;
        destructible = true;    // 有血量组件（实际所有伤害被拦截）
        breakable = false;      // 禁止任何右键拆除（仅塔塔近身触发摧毁）
        health = 3000;
        size = 1;
        group = BlockGroup.projectors;
        requirements(Category.defense, ItemStack.empty);
    }

    public class ErosionCoreBuild extends Building {
        /** 塔塔近身累计时间（tick），塔塔离开后缓慢衰减 */
        private float tataProgress = 0f;

        @Override
        public void updateTile() {
            // 检测附近是否有友方塔塔
            boolean tataNearby = false;
            for (Unit u : Groups.unit) {
                if (u.type == EFunits.tata && u.team == team && u.within(this, 15f)) {
                    tataNearby = true;
                    break;
                }
            }

            if (tataNearby) {
                tataProgress += Time.delta;
                if (tataProgress >= TATA_DESTROY_TIME) {
                    // 摧毁完成
                    kill();
                }
            } else if (tataProgress > 0f) {
                // 塔塔离开 → 进度缓慢衰减
                tataProgress = Math.max(tataProgress - Time.delta * 0.5f, 0f);
            }
        }

        @Override
        public float handleDamage(float amount) {
            // 免疫所有武器/爆炸伤害
            return 0f;
        }

    }
}
