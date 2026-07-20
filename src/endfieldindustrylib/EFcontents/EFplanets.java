package endfieldindustrylib.EFcontents;

import arc.graphics.Color;
import mindustry.content.Blocks;
import static mindustry.content.Planets.sun;
import mindustry.game.Team;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.PlanetGrid;
import mindustry.graphics.g3d.SunMesh;
import mindustry.maps.planet.ErekirPlanetGenerator;
import mindustry.type.Planet;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Env;

public class EFplanets {
    public static void loadContents() {
        // ========== 1. 创建气态巨行星 "塔罗斯" ==========
        Planet talos = new Planet("talos", sun, 3f, 4) {{
            orbitTime = 60f * 3600;
            rotateTime = 60f * 60f * 4;
            orbitSpacing = 5f;
            radius = 3f;
            minZoom = 0.3f;
            maxZoom = 2f;

            visible = true;
            drawOrbit = true;
            bloom = false;                          // 非恒星不要 bloom
            lightColor = Color.valueOf("88aaff");
            iconColor = Color.valueOf("88aaff");
            icon = "icon-talos";
                solarSystem = this; 
            meshLoader = () -> new SunMesh(
                this, 4,
                5, 0.3, 1.7, 1.2, 1,
                1.1f,
                Color.valueOf("7ec8e3"),
                Color.valueOf("5ab0d8"),
                Color.valueOf("91d5e8"),
                Color.valueOf("b5e2f0"),
                Color.valueOf("d4f0f7"),
                Color.valueOf("e8f8fc")
            );
            orbitRadius = 180f;                      // 远离太阳

            grid = null;
            generator = null;
            updateLighting = true;
            lightSrcFrom = 0.8f;
            lightSrcTo = 1.2f;

            hasAtmosphere = true;
            atmosphereColor = Color.valueOf("6a7a9f");
            atmosphereRadIn = 0.95f;
            atmosphereRadOut = 1.15f;
        }};

        // ========== 2. 创建卫星 "塔卫二"（可登陆 + 战役）==========
        new Planet("taelos-II", talos, 0.6f, 3) {{
            orbitTime = 60f * 300;
            rotateTime = orbitTime;                 // 潮汐锁定
            orbitSpacing = 8f;                      // 增大间距，更像地日比例
            radius = 0.6f;
            minZoom = 0.5f;
            maxZoom = 4f;

            // —— 外观 ——
            meshLoader = () -> new HexMesh(this, 5);
            cloudMeshLoader = () -> null;           // 无云（或可加 HexSkyMesh）

            // —— 可登陆要素 ——
            generator = new ErekirPlanetGenerator();
            grid = PlanetGrid.create(2);
            sectorSeed = 12345;
            sectorApproxRadius = 1.8f;

            // —— 战役要素 ——
            accessible = true;
            alwaysUnlocked = true;                  // 始终可见
            startSector = 0;                        // 起始区块（根据网格调整）
            allowCampaignRules = true;
            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
            allowSectorInvasion = true;
            clearSectorOnLose = true;               // 失守后重置
            defaultCore = Blocks.coreBastion;

            defaultEnv = Env.terrestrial | Env.oxygen | Env.scorching;
            defaultAttributes.set(Attribute.heat, 0.8f);

            // 战役规则（与 Erekir 类似）
            ruleSetter = r -> {
                r.waveTeam = Team.malis;
                r.placeRangeCheck = false;
                r.hideSpawns = false;
                r.fog = true;
                r.staticFog = true;
                r.lighting = false;
                r.coreDestroyClear = true;
                r.onlyDepositCore = true;
            };

            // —— 大气 / 光照 ——
            hasAtmosphere = true;
            atmosphereColor = Color.valueOf("d4b48c");
            landCloudColor = Color.valueOf("b5977a88");
            tidalLock = true;
            updateLighting = true;
            solarSystem = talos;                    // ← 新增：以塔罗斯为光照中心
            tidalLock = true;                       // 潮汐锁定，始终一面朝向塔罗斯
            updateLighting = true;
            lightSrcFrom = 0.0f;                    // 白天起始更暗
            lightSrcTo = 0.4f;                      // 白天峰值更低
            lightDstFrom = 0.1f;                    // 夜晚更黑
            lightDstTo = 0.6f;

            visible = true;
            drawOrbit = true;
            iconColor = Color.valueOf("b5977a");
            icon = "icon-taelos-II";
        }};
    }
}