package endfieldindustrylib.EFcontents;

import endfieldindustrylib.EFworld.CampaignHandler;
import mindustry.type.SectorPreset;

/**
 * 塔卫二战役关卡预设。
 * <p>
 * 每个关卡对应一个扇区，使用默认的 FileMapGenerator 加载地图文件。
 * 后续需要将对应的 .msav 地图文件放入 {@code assets/maps/} 目录下，
 * 文件名需与 SectorPreset 名称一致（例如 {@code theHub.msav}）。
 * <p>
 * 扇区坐标（构造参数中的数字）为行星网格上的位置索引，
 * 需与后续制作的 .msav 地图文件中保存的坐标一致。
 */
public class EFsectorPresets {
    public static SectorPreset
        theHub,
        theHubII,
        originiumSciencePark,
        originLodespring,
        powerPlateau,
        originiumScienceParkII,
        originLodespringII,
        powerPlateauII;

    public static void load() {
        // ===================================================================
        //  枢纽区（新手教程）
        // ===================================================================
        // 坐标 0 — 需地图文件: assets/maps/theHub.msav
        theHub = new SectorPreset("theHub", EFplanets.taelosII, 0) {{
            // 教程关：防御战，只有几个简单的敌人，
            // 检测到玩家的防御塔数量达到一定数量后触发下一波
            alwaysUnlocked = true;
            addStartingItems = true;
            //noLighting = true;
            startWaveTimeMultiplier = 3f;
            captureWave = 15;
            difficulty = 1;
            rules = r -> {
                r.winWave = captureWave;
                r.mission = "建造至少两座铳械塔";
                r.initialWaveSpacing = 999f * 60f * 2f;   // 初始波次间隔极大，等待玩家建成双管炮
                CampaignHandler.theHubRules(this);
            };
        }};
        // 坐标 1 — 需地图文件: assets/maps/theHub-ii.msav
        theHubII = new SectorPreset("theHub-ii", EFplanets.taelosII, 1) {{
            // 教程关：按照教程摆下几个工厂，
            // 收集五百个晶体外壳和源矿后通关
            addStartingItems = true;
            startWaveTimeMultiplier = 3f;
            noLighting = true;
            difficulty = 3;
            rules = r -> {
                r.winWave = 0;
                r.mission = "收集 500 源矿与 500 晶体外壳";
                CampaignHandler.theHubIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.theHub.sector);
        }};

        // ===================================================================
        //  源石研究所
        // ===================================================================
        originiumSciencePark = new SectorPreset("originiumSciencePark", EFplanets.taelosII, 15) {{
            // 抵御10轮敌人，保护一个单位从地图角落回到核心旁边，
            // 单位死亡则失败，一半敌人会攻击护送单位
            captureWave = 30;
            noLighting = true;
            difficulty = 5;
            rules = r -> {
                r.winWave = captureWave;
                r.mission = "保护护送单位安全到达核心";
                CampaignHandler.originiumScienceParkRules(this);
            };
            shieldSectors.add(EFsectorPresets.theHub.sector);
        }};
        originiumScienceParkII = new SectorPreset("originiumSciencePark-ii", EFplanets.taelosII, 16) {{
            // 保证某些物品产量达标，零星敌人不断涌来
            difficulty = 5;
            noLighting = true;
            rules = r -> {
                r.winWave = 0;
                r.mission = "收集 300 源石外壳";
                CampaignHandler.originiumScienceParkIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.originiumSciencePark.sector);
        }};

        // ===================================================================
        //  矿脉园区
        // ===================================================================
        // 坐标 30 — 需地图文件: assets/maps/originLodespring.msav
        originLodespring = new SectorPreset("originLodespring", EFplanets.taelosII, 30) {{
            // 护送单位到达地图另一边，无核心，
            // 单位死亡即失败，敌人不断涌向目标
            difficulty = 7;
            noLighting = true;
            rules = r -> {
                r.winWave = 0;
                r.mission = "护送单位穿越矿脉园区";
                CampaignHandler.originLodespringRules(this);
            };
            shieldSectors.add(EFsectorPresets.originiumSciencePark.sector);
        }};
        // 坐标 31 — 需地图文件: assets/maps/originLodespring-ii.msav
        originLodespringII = new SectorPreset("originLodespring-ii", EFplanets.taelosII, 31) {{
            // 护送单位前往三个区域破坏侵蚀核，
            // 每破坏一个侵蚀核削弱最终Boss
            difficulty = 7;
            noLighting = true;
            rules = r -> {
                r.winWave = 0;
                r.mission = "摧毁三处侵蚀核，击败最终 Boss";
                CampaignHandler.originLodespringIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.originLodespring.sector);
        }};

        // ===================================================================
        //  供能高地（最终战役）
        // ===================================================================
        // 坐标 45 — 需地图文件: assets/maps/powerPlateau.msav
        powerPlateau = new SectorPreset("powerPlateau", EFplanets.taelosII, 45) {{
            // 限时护送单位到指定位置，
            // 挖掘侵蚀核增加倒计时，单位死亡即失败，
            // 胜利时单位同时破坏大量侵蚀核，自身牺牲
            difficulty = 9;
            noLighting = true;
            rules = r -> {
                r.winWave = 0;
                r.mission = "限时护送单位到达目标位置";
                CampaignHandler.powerPlateauRules(this);
            };
            shieldSectors.add(EFsectorPresets.originLodespring.sector);
        }};
        // 坐标 46 — 需地图文件: assets/maps/powerPlateau-ii.msav
        powerPlateauII = new SectorPreset("powerPlateau-ii", EFplanets.taelosII, 46) {{
            // 摧毁地图四角炮台→Boss出现→击败Boss胜利
            difficulty = 9;
            isLastSector = true;
            noLighting = true;
            rules = r -> {
                r.winWave = 0;
                r.mission = "摧毁四角炮台，击败最终 Boss";
                CampaignHandler.powerPlateauIIRules(this);
            };
            shieldSectors.add(EFsectorPresets.powerPlateau.sector);
        }};
    }
}
