package endfieldindustrylib.EFcontents;

import endfieldindustrylib.EFworld.ai.FollowAI;
import endfieldindustrylib.EFworld.unit.Ram;
import endfieldindustrylib.EFworld.unit.Tata;
import mindustry.ai.UnitCommand;
import mindustry.gen.Unit;
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
    public static UnitType ram;
    /** 自定义"跟随玩家"命令 */
    public static UnitCommand followCommand;

    public static void load() {
        // 创建自定义跟随命令
        followCommand = new UnitCommand("follow", "players", (Unit u) -> new FollowAI()) {{
            switchToMove = false;   // 不因右键点击而切换到移动
        }};

        // 实例化塔塔（具体定义见 EFworld.unit.Tata）
        tata = new Tata("tata");
        ram = new Ram("ram");
    }
}

