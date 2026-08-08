package endfieldindustrylib.EFworld.unit;

import arc.Core;
import arc.struct.Seq;
import endfieldindustrylib.EFcontents.EFunits;
import mindustry.ai.UnitCommand;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import static mindustry.gen.Sounds.explosionReactor;
import mindustry.gen.TankUnit;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

/**
 * 塔塔——所有战役关卡中需要护送的单位。
 * <p>
 * 塔塔是地面单位，血量较高，移速极慢，无武器但有建造能力，
 * 且是唯一可以拆除"侵蚀核"的单位（侵蚀核的逻辑检查塔塔的类型）。
 * 定义从原 EFunits 内联块迁移至此。
 */
public class Tata extends UnitType {

    public Tata(String name) {
        super(name);

        // —— 基础属性 ——
        health = 2500f;
        speed = 0.15f;
        hitSize = 16f;
        armor = 2f;
        drag = 0.4f;
        accel = 0.3f;
        rotateSpeed = 3f;
        // —— 地面单位 ——
        flying = false;
        physics = true;                 // 启动物理碰撞
        hovering = false;               // 不悬浮，受地面影响
        canDrown = true;                // 可在深水中淹死
        canBoost = false;               // 不能起飞
        omniMovement = false;            // 需转向移动，产生转头效果

        // —— 建造能力 ——
        buildSpeed = 4.0f;
        buildRange = 240f;

        // —— 游戏属性 ——
        isEnemy = false;
        playerControllable = true;
        logicControllable = true;
        useUnitCap = true;
        hoverable = true;
        // —— 命令模式 ——
        commands = Seq.with(
            UnitCommand.moveCommand,
            UnitCommand.rebuildCommand,
            UnitCommand.assistCommand,
            EFunits.followCommand
        );
        defaultCommand = UnitCommand.moveCommand;

        // —— 显示 ——
        drawBody = true;
        drawCell = true;
        drawItems = true;

        constructor = TankUnit::create;
    }

    // —— 死亡爆炸（等同于钍反应堆） ——
    @Override
    public void killed(Unit unit) {
        Fx.reactorExplosion.at(unit.x, unit.y);
        Effect.shake(6f, 16f, unit);
        Damage.damage(null, unit.x, unit.y, 19f * 8f, 5000f, true, true, true);
        explosionReactor.at(unit, 1f);
    }

    // —— 临时：使用耀星(Quasar)的贴图 ——
    @Override
    public void load() {
        super.load();
        region = Core.atlas.find("quasar");
    }
}
