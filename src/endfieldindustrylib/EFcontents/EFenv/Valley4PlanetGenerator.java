package endfieldindustrylib.EFcontents.EFenv;

import arc.graphics.Color;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec3;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.content.Loadouts;
import mindustry.game.Schematics;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.TileGen;

/** 四号谷地枢纽区 — 修正版：高原、草地、墙体均衡 */
public class Valley4PlanetGenerator extends PlanetGenerator {

    float heightScl = 1.0f;
    float heightPow = 2.0f;
    float heightMult = 2.2f;


    Block[] terrain = {
        Blocks.sand,               // 0.000 - 0.125
        Blocks.sand,              // 0.125 - 0.250
        Blocks.grass,              // 0.250 - 0.375
        Blocks.grass,              // 0.375 - 0.500
        Blocks.grass,
        Blocks.grass,               // 0.500 - 0.625
        Blocks.grass,
        Blocks.grass,
        Blocks.grass, 
        Blocks.sand,
        Blocks.stone,
        Blocks.stone,              // 0.875 - 1.000 （高峰）
    };

    {
        baseSeed = 6;
        defaultLoadout = Loadouts.basicShard;
    }

    @Override
    public float getHeight(Vec3 position) {
        return Mathf.pow(rawHeight(position), heightPow) * heightMult;
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        out.set(Color.valueOf("5cb85c"));
    }

    @Override
    public float getSizeScl() {
        return 5000f;
    }

    float rawHeight(Vec3 position) {
        float main = Simplex.noise3d(seed, 7, 0.6f, 1f / heightScl, position.x, position.y, position.z);
        float detail = Simplex.noise3d(seed + 1, 4, 0.5f, 1f / 0.3f, position.x + 100f, position.y, position.z + 50f);
        return main * 0.75f + detail * 0.25f;
    }

    Block getBlock(Vec3 position) {
        // 关键修复：除以 1.8 保留陡峭高峰，振幅约 ±1.3，经 clamp 后仍能产生石头和沙地
        float raw = rawHeight(position);
        float scaled = raw /1.8f;
        float h = Mathf.clamp(scaled * 0.5f + 0.5f, 0f, 1f);
        return terrain[Mathf.clamp((int) (h * terrain.length), 0, terrain.length - 1)];
    }

    @Override
    public void genTile(Vec3 position, TileGen tile) {
        tile.floor = getBlock(position);
        tile.block = Blocks.air;

        if (tile.floor == Blocks.grass && rand.chance(0.03)) {
            tile.block = Blocks.shrubs;
        }
    }

    int[] findSpawn(int cx, int cy, int range) {
        for (int r = 0; r <= range; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    int x = cx + dx, y = cy + dy;
                    if (!tiles.in(x, y)) continue;
                    Tile t = tiles.getn(x, y);
                    if (t.block() == Blocks.air && !t.floor().isLiquid) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        return new int[]{cx, cy};
    }

    @Override
    protected void generate() {
        int cx = width / 2, cy = height / 2;
        int spawnX = cx + rand.random(-width / 6, width / 6);
        int spawnY = cy + rand.random(-height / 6, height / 6);

        // ===== 河流（~55% 概率）=====
        boolean hasRiver = noise(cx, cy, 1, 1f, 200f, 1f) > 0.45f;
        if (hasRiver) {
            pass((x, y) -> {
                float rn = noise(x + 200, y + 300, 5, 0.6f, 120f, 1f);
                float riverDist = Math.abs(rn - 0.5f) * 2f;
                if (riverDist < 0.04f) {
                    floor = Blocks.water;
                    block = Blocks.air;
                    ore = Blocks.air;
                } else if (riverDist < 0.08f) {
                    if (floor != Blocks.water) {
                        floor = Blocks.sand;
                        block = Blocks.air;
                    }
                }
            });
        }

        cells(2);

        // ===== 山地边缘破碎（stone→sand 过渡）=====
        pass((x, y) -> {
            if (floor == Blocks.stone) {
                if (noise(x + 300, y + 400, 4, 0.6f, 25f, 1f) < 0.35f) {
                    floor = Blocks.sand;
                }
            }
        });

        // ===== 连续墙体（条件宽松，高峰密集区）=====
        boolean[][] wallSeed = new boolean[width][height];
        for (Tile tile : tiles) {
            int x = tile.x, y = tile.y;
            if (floorAt(x, y) == Blocks.stone && noise(x, y + 100, 3, 0.7f, 18f, 1f) > 0.55f) {
                wallSeed[x][y] = true;
            }
        }
        for (Tile tile : tiles) {
            int x = tile.x, y = tile.y;
            if (floorAt(x, y) != Blocks.stone) continue;
            int count = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dx, ny = y + dy;
                    if (tiles.in(nx, ny) && wallSeed[nx][ny]) count++;
                }
            }
            if (count >= 2 && wallSeed[x][y]) {
                tiles.getn(x, y).setBlock(Blocks.stoneWall);
            }
        }

        // ===== 额外散落墙体与巨石（增加山地细节）=====
        pass((x, y) -> {
            if (floor == Blocks.stone && block == Blocks.air) {
                if (rand.chance(0.004)) {
                    block = Blocks.stoneWall;      // 散落墙
                } else if (rand.chance(0.0125)) {
                    block = Blocks.boulder;        // 散落巨石
                }
            }
        });

        blend(Blocks.stone, Blocks.sand, 3);

        // ===== 植被细节 =====
        //pass((x, y) -> {
        //    if (block == Blocks.air) {
                //if (floor == Blocks.grass && rand.chance(0.06)) {
                //    block = Blocks.shrubs;
               // }
                // 给沙地偶尔加点枯草（装饰）
                //if (floor == Blocks.sand && rand.chance(0.02)) {
                //    block = Blocks.shrubs;
                //}
        //    }
        //});

        trimDark();

        // ===== 强制清理：grass 和 sand 上不允许有任何墙/非装饰方块（防止“绿色墙体”）=====
        //for (Tile tile : tiles) {
        //    Block f = tile.floor();
        //    Block b = tile.block();
        //    if ((f == Blocks.grass || f == Blocks.sand) && b != Blocks.air && b != Blocks.shrubs) {
       //         tile.setBlock(Blocks.air);
       //     }
       // }

        distort(10f, 12f);

        // ===== 核心 =====
        int[] spawn = findSpawn(spawnX, spawnY, 25);
        spawnX = spawn[0];
        spawnY = spawn[1];
        inverseFloodFill(tiles.getn(spawnX, spawnY));
        erase(spawnX, spawnY, 15);
        Schematics.placeLaunchLoadout(spawnX, spawnY);

        // ===== 敌人 =====
        int enemyCount = rand.random(5, 9);
        for (int i = 0; i < enemyCount; i++) {
            float a = 360f / enemyCount * i + rand.random(-20f, 20f);
            float d = Math.min(width, height) * 0.44f;
            int ex = Mathf.clamp((int) (cx + Angles.trnsx(a, d)), 5, width - 5);
            int ey = Mathf.clamp((int) (cy + Angles.trnsy(a, d)), 5, height - 5);
            Tile tile = tiles.getn(ex, ey);
            if (tile != null) {
                tile.setOverlay(Blocks.spawn);
                erase(ex, ey, 3);
            }
        }

        // ===== 矿物（占位） =====
        pass((x, y) -> {
            if (block == Blocks.air && !nearWall(x, y)) {
                if (noise(x + 150, y + 200, 4, 0.7f, 50f, 1f) > 0.72f) {
                    // TODO: EFitems.originiumOre
                }
                if (noise(x + 999, y + 600, 5, 0.6f, 45f, 1f) > 0.82f) {
                    // TODO: EFitems.amethystOre
                }
            }
        });
    }

    Block floorAt(int x, int y) {
        return tiles.in(x, y) ? tiles.getn(x, y).floor() : Blocks.air;
    }
}