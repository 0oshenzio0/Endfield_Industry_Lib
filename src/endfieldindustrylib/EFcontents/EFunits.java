package endfieldindustrylib.EFcontents;

import arc.struct.Seq;
import mindustry.ai.UnitCommand;
import mindustry.type.UnitType;

/**
 * 塔卫二自定义单位。
 * <p>
 * 当前仅定义了一个单位——<b>塔塔</b>，即所有战役关卡中需要护送的单位。
 * 塔塔是地面单位，血量较高，移速极慢，无武器但有建造能力，
 * 且是唯一可以拆除"侵蚀核"的单位（侵蚀核的逻辑检查塔塔的类型）。
 */
public class EFunits {
    public static UnitType tata;

    public static void load() {
        tata = new UnitType("tata") {{
            // —— 基础属性 ——
            health = 2500f;
            speed = 0.15f;                       // 非常慢
            hitSize = 22f;                      // T3 体积
            armor = 5f;                         // 少量护甲提升生存
            drag = 0.4f;
            accel = 0.3f;
            rotateSpeed = 3f;

            // —— 地面单位 ——
            flying = false;

            // —— 建造能力 ——
            buildSpeed = 4.0f;                   // >0 启用建造模式
            buildRange = 60f;

            // —— 无武器 ——
            // 不添加任何武器，仅保留默认空 weapons Seq

            // —— 游戏属性 ——
            isEnemy = false;                     // 非敌方单位
            playerControllable = true;
            logicControllable = true;
            useUnitCap = true;
            hoverable = true;

            // —— 命令模式 ——
            // 非飞行地面单位不会自动添加建造/协助命令，需显式声明
            // 默认使用 moveCommand，让玩家能直接右键指挥移动；
            // 需要建造时可从命令 UI 切换到 rebuildCommand / assistCommand
            commands = Seq.with(
                UnitCommand.moveCommand,
                UnitCommand.rebuildCommand,
                UnitCommand.assistCommand
            );
            defaultCommand = UnitCommand.moveCommand;

            // —— 显示 ——
            drawBody = true;
            drawCell = true;
            drawItems = true;
        }};
    }
}
