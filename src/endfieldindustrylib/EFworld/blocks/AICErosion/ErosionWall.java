package endfieldindustrylib.EFworld.blocks.AICErosion;

import arc.util.Time;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

/**
 * 侵蚀墙体 — 依附于 {@link ErosionCore 侵蚀核} 的结构方块。
 * <p>
 * 行为类似 Minecraft 的树叶：如果四邻（上/下/左/右）没有紧贴侵蚀核，
 * 墙体便会开始凋零（持续损失生命值），直至完全崩解。
 * <p>
 * 无法被武器破坏或右键拆除，只能通过"凋零"机制摧毁。
 * 因此地图中的攻略路线是：塔塔拆除侵蚀核 → 墙体失去支撑凋零崩解。
 */
public class ErosionWall extends Block {

    public ErosionWall(String name) {
        super(name);
        // 基础属性
        update = true;          // 需要 Building 实体来检测邻接侵蚀核
        solid = true;
        destructible = true;    // 有血量组件
        breakable = false;      // 禁止右键拆除（仅通过凋零摧毁）
        health = 400;           // 约 10 秒凋零崩解
        size = 1;
        group = BlockGroup.walls;
        requirements(Category.defense, new ItemStack[0]);
    }

    public class ErosionWallBuild extends Building {
        /** 是否正在凋零 */
        private boolean withering = false;

        @Override
        public void updateTile() {
            // 检查四邻是否有侵蚀核
            boolean hasCoreNearby = false;
            for (Building other : proximity) {
                if (other.block instanceof ErosionCore) {
                    hasCoreNearby = true;
                    break;
                }
            }

            if (!hasCoreNearby) {
                // 失去侵蚀核支撑 → 开始凋零
                if (!withering) {
                    withering = true;
                }
                // 直接扣血以绕过 handleDamage（武器免疫）
                health -= 40f * Time.delta / 60f;
                if (health <= 0f && !dead()) {
                    kill();
                }
            } else {
                withering = false;
            }
        }

        @Override
        public float handleDamage(float amount) {
            // 免疫所有武器/爆炸伤害
            return 0f;
        }
    }
}
