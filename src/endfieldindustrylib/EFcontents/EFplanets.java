package endfieldindustrylib.EFcontents;

import arc.graphics.Color;
import endfieldindustrylib.EFcontents.EFenv.Valley4PlanetGenerator;
import mindustry.content.Blocks;
import static mindustry.content.Planets.sun;
import mindustry.game.Team;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.PlanetGrid;
import mindustry.graphics.g3d.SunMesh;
import mindustry.type.Planet;
import mindustry.world.meta.Attribute;
import mindustry.world.meta.Env;

public class EFplanets {
    public static Planet taelosII;

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
        taelosII = new Planet("taelos-II", talos, 0.6f, 3) {{
            orbitTime = 60f * 300;
            rotateTime = orbitTime;                     // 潮汐锁定
            orbitSpacing = 8f;
            radius = 0.6f;
            minZoom = 0.5f;
            maxZoom = 4f;

            // —— 地表网格（使用 Valley4PlanetGenerator 的 getColor 实现地球着色）——
            generator = new Valley4PlanetGenerator();
            grid = PlanetGrid.create(2);
            sectorSeed = 12345;
            sectorApproxRadius = 1.8f;

            meshLoader = () -> new HexMesh(this, 6);
            // —— 云层（白色蓬松云，地球风格）——
            cloudMeshLoader = () -> new MultiMesh(
                new HexSkyMesh(this, 8, 0.15f, 0.13f, 5, Color.white.cpy().a(0.75f), 2, 0.45f, 0.9f, 0.38f),
                new HexSkyMesh(this, 2, 0.6f, 0.16f, 5, Color.white.cpy().a(0.65f), 2, 0.45f, 1f, 0.41f)
            );

            // —— 战役要素 ——
            accessible = true;
            alwaysUnlocked = true;
            allowLaunchToNumbered = true;
            startSector = 0;
            allowCampaignRules = true;
            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
            allowSectorInvasion = true;
            clearSectorOnLose = true;
            defaultCore = EFblocks.aicCore;

            // —— 生态：温带含氧地球化环境 ——
            defaultEnv = Env.terrestrial | Env.oxygen | Env.groundWater;
            defaultAttributes.set(Attribute.water, 0.5f);

            // —— 战役规则 ——
            ruleSetter = r -> {
                r.waveTeam = Team.malis;
                r.placeRangeCheck = false;
                r.hideSpawns = false;
                r.fog = true;
                r.staticFog = true;
                r.lighting = true;
                r.coreDestroyClear = true;
                r.onlyDepositCore = true;
            };

            // —— 大气：地球蓝色天空 + 白色云层 ——
            hasAtmosphere = true;
            atmosphereColor = Color.valueOf("4a90d9");      // 地球蓝天空
            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;
            landCloudColor = Color.valueOf("ffffffaa");     // 白色半透云

            // —— 光照 & 轨道 ——
            tidalLock = true;
            updateLighting = true;
            solarSystem = talos;
            lightSrcFrom = 0.0f;
            lightSrcTo = 0.8f;
            lightDstFrom = 0.2f;
            lightDstTo = 1.0f;

            // —— 可见性 ——
            visible = true;
            drawOrbit = true;
            iconColor = Color.valueOf("4a90d9");
            icon = "icon-taelos-II";
        }};
    }
}