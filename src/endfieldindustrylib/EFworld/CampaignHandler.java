package endfieldindustrylib.EFworld;

import arc.Events;
import arc.util.Time;
import static mindustry.Vars.state;
import mindustry.content.Blocks;
import mindustry.game.EventType.BlockBuildEndEvent;
import mindustry.game.EventType.GameOverEvent;
import static mindustry.game.EventType.Trigger.update;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WaveEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.MapObjectives.BuildCountObjective;
import mindustry.game.MapObjectives.CoreItemObjective;
import mindustry.game.MapObjectives.DestroyCoreObjective;
import mindustry.game.MapObjectives.FlagObjective;
import mindustry.game.MapObjectives.TimerObjective;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.SectorPreset;
import mindustry.type.UnitType;

/**
 * 塔卫二战役自定义规则与事件处理器。
 * <p>
 * 每个关卡通过对应的 {@code *Rules(SectorPreset)} 方法在其 SectorPreset.rules 中被调用，
 * 配置自定义 Rules 和 MapObjectives。
 * 静态 {@link #init()} 在 Mod.init() 中调用，注册全局事件监听以驱动自定义胜负判定。
 */
public class CampaignHandler {

    // ======================== 全局状态跟踪 ========================

    /** 玩家已建造的防御炮塔总数 */
    private static int turretCount = 0;
    /** 当前关卡是否处于护送模式（有需要保护的 escortUnit） */
    private static boolean escortActive = false;
    /** 护送单位的类型（用于在地图上识别） */
    private static UnitType escortUnitType = null;
    /** 护送单位需到达的目标 tile X（地图坐标） */
    private static int escortTargetX = -1;
    /** 护送单位需到达的目标 tile Y（地图坐标） */
    private static int escortTargetY = -1;
    /** 目标判定半径（格数） */
    private static float escortTargetRadius = 8f;
    /** 是否有任务目标系统正在运行（非波次关） */
    private static boolean objectivesMode = false;

    // ======================== 工具方法 ========================

    /** 是否为塔卫二战役模式 */
    public static boolean isCampaign() {
        return state.rules.sector != null
            && state.rules.sector.planet == endfieldindustrylib.EFcontents.EFplanets.taelosII;
    }

    /** 检查当前关卡的 MapObjectives 是否全部完成 */
    private static boolean allObjectivesComplete() {
        for (var obj : state.rules.objectives) {
            if (!obj.isCompleted()) return false;
        }
        return state.rules.objectives.all.size > 0;
    }

    /** 在目标关卡设置护送参数 */
    private static void setupEscort(UnitType unitType, int targetX, int targetY, float radius) {
        escortActive = true;
        escortUnitType = unitType;
        escortTargetX = targetX;
        escortTargetY = targetY;
        escortTargetRadius = radius;
    }

    /** 重置当前关卡状态 */
    private static void resetState() {
        turretCount = 0;
        escortActive = false;
        escortUnitType = null;
        escortTargetX = -1;
        escortTargetY = -1;
        escortTargetRadius = 8f;
        objectivesMode = false;
    }

    // ===================================================================
    //  公共入口：在每个 SectorPreset.rules{} 中调用
    // ===================================================================

    /** 枢纽区 — 教程防御战（建双管炮→十五波） */
    public static void theHubRules(SectorPreset preset) {
        state.rules.objectives.add(
            new BuildCountObjective(Blocks.duo, 2)
                .details("建造两座双管炮来开始战斗")
        );
    }

    /** 枢纽区II — 工厂教程，收集资源通关 */
    public static void theHubIIRules(SectorPreset preset) {
        objectivesMode = true;
        // 按顺序收集：500 源矿 → 500 紫水晶纤维
        state.rules.objectives.add(
            new CoreItemObjective(endfieldindustrylib.EFcontents.EFitems.originiumOre, 500)
                .details("收集 500 个源矿")
                .child(new CoreItemObjective(endfieldindustrylib.EFcontents.EFitems.amethystFiber, 500)
                    .details("收集 500 个紫水晶纤维"))
        );
    }

    /** 源石研究所 — 护送 + 防守（波次胜利 + 护送存活） */
    public static void originiumScienceParkRules(SectorPreset preset) {
        // 标记护送单位类型（由地图生成，需在 .msav 中放置塔塔）
        setupEscort(
            endfieldindustrylib.EFcontents.EFunits.tata,
            0, 0, 15f                          // 核心位置（地图中心附近），由地图决定
        );
        state.rules.objectives.add(
            new FlagObjective("escort_safe", null)
                .details("保护护送单位安全返回核心")
                .flagsAdded("escort_safe")
        );
    }

    /** 源石研究所II — 产量达标 */
    public static void originiumScienceParkIIRules(SectorPreset preset) {
        objectivesMode = true;
        state.rules.objectives.add(
            new CoreItemObjective(endfieldindustrylib.EFcontents.EFitems.origocrust, 300)
                .details("收集 300 个源石外壳")
                .child(new CoreItemObjective(endfieldindustrylib.EFcontents.EFitems.ferrium, 200)
                    .details("收集 200 个铁锭"))
                .child(new CoreItemObjective(endfieldindustrylib.EFcontents.EFitems.carbon, 200)
                    .details("收集 200 个碳"))
        );
    }

    /** 矿脉园区 — 无核心护送穿越 */
    public static void originLodespringRules(SectorPreset preset) {
        objectivesMode = true;
        state.rules.attackMode = true;
        // 标记护送单位和目标坐标（需在地图中放置）
        setupEscort(
            endfieldindustrylib.EFcontents.EFunits.tata,
            -1, -1, 12f                        // 目标位置由地图决定
        );
        state.rules.objectives.add(
            new FlagObjective("escort_arrived", null)
                .details("护送单位到达目标位置")
                .flagsAdded("escort_arrived")
        );
    }

    /** 矿脉园区II — 破坏侵蚀核→最终Boss */
    public static void originLodespringIIRules(SectorPreset preset) {
        objectivesMode = true;
        state.rules.attackMode = true;
        // 使用 DestroyCoreObjective 检测所有敌方核心（侵蚀核用 CoreBlock 标记）
        // 三个侵蚀核和 Boss 核心需在 .msav 地图中放置
        state.rules.objectives.add(
            new DestroyCoreObjective()
                .details("摧毁三个区域的侵蚀核，击败最终 Boss")
        );
    }

    /** 供能高地 — 限时护送 */
    public static void powerPlateauRules(SectorPreset preset) {
        objectivesMode = true;
        setupEscort(
            endfieldindustrylib.EFcontents.EFunits.tata,
            -1, -1, 10f
        );
        state.rules.objectives.add(
            new TimerObjective("剩余时间: {0}", 180f)
                .details("在倒计时结束前护送单位到达目标位置")
                .child(new FlagObjective("ending_triggered", null)
                    .details("等待结局演出"))
        );
    }

    /** 供能高地II — 最终决战（摧毁炮台→Boss） */
    public static void powerPlateauIIRules(SectorPreset preset) {
        objectivesMode = true;
        state.rules.attackMode = true;
        // 四角炮台用 DestroyBlocksObjective（坐标需在地图中确定后填入）
        // Boss 核心用 DestroyCoreObjective 检测
        state.rules.objectives.add(
            new DestroyCoreObjective()
                .details("摧毁地图四角的敌方炮台，击败最终 Boss")
        );
    }

    // ===================================================================
    //  事件监听器 — 在 Mod.init() 中注册
    // ===================================================================

    /** 注册全局战役事件监听 */
    public static void init() {
        // ── 每帧更新：检查目标完成状态 → 触发胜利 ──
        Events.run(update, () -> {
            if (!isCampaign()) return;

            // objectivesMode 关卡：全部目标完成时触发胜利
            if (objectivesMode && allObjectivesComplete()) {
                Call.sectorCapture();
                return;
            }

            // 护送模式：检查护送单位是否到达目标
            if (escortActive && escortUnitType != null) {
                checkEscortArrival();
            }
        });

        // ── 波次事件：用于后续扩展（如炮塔加成） ──
        Events.on(WaveEvent.class, event -> {
            if (!isCampaign()) return;
            // 预留：此处可添加根据炮塔数量增加敌人规模等逻辑
        });

        // ── 建筑建造事件：统计炮塔数量 + 触发 theHub 波次 ──
        Events.on(BlockBuildEndEvent.class, event -> {
            if (!isCampaign()) return;
            if (!event.breaking && event.tile != null && event.tile.block() != null
                && event.tile.block().attacks) {
                turretCount++;

                // theHub：建成两座双管炮后恢复波次间隔
                String name = state.rules.sector.preset != null
                    ? state.rules.sector.preset.name : "";
                if ("theHub".equals(name) && turretCount >= 2) {
                    state.rules.waveSpacing = 2f * Time.toMinutes;
                }
            }
        });

        // ── 单位摧毁事件：检测护送单位死亡 → 任务失败 ──
        Events.on(UnitDestroyEvent.class, event -> {
            if (!isCampaign()) return;
            if (!escortActive || escortUnitType == null) return;

            Unit u = event.unit;
            if (u != null && u.type == escortUnitType && u.team == state.rules.defaultTeam) {
                // 护送单位被摧毁 → 触发失败
                escortActive = false;
                if (!state.gameOver) {
                    state.gameOver = true;
                    Events.fire(new GameOverEvent(state.rules.waveTeam));
                }
            }
        });

        // ── 加载关卡时重置状态 ──
        Events.on(WorldLoadEvent.class, event -> {
            resetState();
        });
    }

    // ======================== 护送逻辑 ========================

    /** 每帧检查护送单位（塔塔）是否到达目标区域 */
    private static void checkEscortArrival() {
        if (!escortActive || escortUnitType == null) return;
        if (escortTargetX < 0 || escortTargetY < 0) return; // 坐标未设置

        // 遍历玩家队伍中塔塔类型的单位
        for (Unit u : Groups.unit) {
            if (u.team == state.rules.defaultTeam && u.type == escortUnitType) {
                float dx = u.x - escortTargetX * 8f; // tile→world 转换
                float dy = u.y - escortTargetY * 8f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist <= escortTargetRadius * 8f) {
                    // 到达目标区域 → 根据当前关卡设置对应的 objective flag
                    if (state.rules.sector.preset != null) {
                        String name = state.rules.sector.preset.name;
                        if ("originiumSciencePark".equals(name)) {
                            state.rules.objectiveFlags.add("escort_safe");
                        } else if ("originLodespring".equals(name)) {
                            state.rules.objectiveFlags.add("escort_arrived");
                        } else if ("powerPlateau".equals(name)) {
                            state.rules.objectiveFlags.add("ending_triggered");
                        }
                    }
                    escortActive = false;
                    break;
                }
            }
        }
    }
}
