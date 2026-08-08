package endfieldindustrylib.EFworld.ai;

import arc.graphics.Color;
import arc.util.Time;
import static mindustry.Vars.headless;
import static mindustry.Vars.tilesize;
import mindustry.ai.types.GroundAI;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.gen.Unit;
import mindustry.world.blocks.environment.Floor;

/**
 * 拉姆（Ram）专属 AI — 基于 GroundAI 完全重写。
 * <p>
 * 行为：15 格（{@link #detectRange}）内发现地面敌人则转为攻击模式
 * （面向敌人并贴近到前颚挥击命中距离，贴身由武器自动挥击），
 * 否则交由 {@link GroundAI} 标准逻辑朝核心寻路奔跑。
 * 所有移动均走标准 AI 移动链路（{@code pathfind}/{@code moveTo} → {@code movePref} → {@code rotateMove}），
 * 因此继承 GroundAI 的寻路绕障、卡住检测与面向逻辑，不再自行驱动 {@code vel}。
 */
public class RamAI extends GroundAI {
    /** 索敌半径（世界单位）：15 格 */
    private static final float detectRange = 15f * tilesize;
    /** 前颚挥击命中半径（世界单位，与 splashDamageRadius 一致） */
    private static final float meleeRange = 8f;
    /** 近战贴近的减速距离（世界单位）：越大越早减速，越小冲撞越猛 */
    private static final float attackSmooth = 40f;
    /** 奔跑扬尘计时（帧） */
    private float dustTimer = 0f;

    @Override
    public void updateMovement(){
        // —— 15 格索敌：发现地面敌人则攻击，否则朝核心寻路（标准 GroundAI） ——
        Unit enemy = Units.closestEnemy(unit.team, unit.x, unit.y, detectRange, u -> u.checkTarget(false, true));

        if(enemy != null){
            attackMove(enemy);
        }else{
            super.updateMovement();
        }

        kickDust();
    }

    /** 攻击模式：面向敌人并贴近到前颚挥击命中距离，贴身由武器自动挥击 */
    private void attackMove(Unit enemy){
        // 挥击命中距离：前颚挥击半径 + 目标命中体积的一半
        float hitRange = meleeRange + enemy.hitSize * 0.5f;

        // 贴近到命中距离停下（moveTo 内部走 rotateMove 转向移动；贴身自动停住）
        moveTo(enemy, hitRange, attackSmooth);

        // 面向敌人：保证前颚朝向目标，跑姿与攻击朝向一致
        unit.lookAt(enemy);
    }

    /** 扬起沙尘（约每 0.12 秒一团） */
    private void kickDust(){
        if((dustTimer += Time.delta) >= 7f){
            dustTimer = 0f;
            if(!headless){
                Floor floor = unit.floorOn();
                float dustScale = unit.type.hitSize / 8f;
                if(floor != null){
                    floor.walkEffect.at(unit.x, unit.y, dustScale, floor.mapColor);
                }else{
                    Fx.unitLandSmall.at(unit.x, unit.y, dustScale, Color.white);
                }
            }
        }
    }

}
