package endfieldindustrylib.EFworld.blocks.AICTurret;

//射线塔
import arc.graphics.Color;
import endfieldindustrylib.EFcontents.EFitems;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.turrets.ItemTurret;

public class BeamTower extends ItemTurret {
    public BeamTower(String name) {
        super(name);

        requirements(Category.turret, ItemStack.with(EFitems.origocrust, 25));

        consumePower(20f);

        size = 2;
        range = 100f;       // 12.5m × 8 = 100
        reload = 480f;      // 8s
        rotateSpeed = 3f;
        inaccuracy = 0f;
        maxAmmo = 10;
        ammoPerShot = 1;
        shootSound = Sounds.shootLaser;

        targetAir = true;
        targetGround = true;

        ammo(
            EFitems.origocrust, new BasicBulletType(8f, 11169) {{ // 速度改为 1格/帧（8f），half of 22338
                width = 6f;
                height = 20f;
                lifetime = 12.5f; // 100/8 = 12.5，确保 speed×lifetime=range
                keepVelocity = true;
                collides = true;
                collidesTiles = false;
                pierce = false;
                // TODO: replace with LaserBulletType / beam effect
            }},
            EFitems.amethystFiber, new BasicBulletType(8f, 22338) {{ // 速度改为 1格/帧（8f）
                width = 6f;
                height = 20f;
                lifetime = 12.5f; // 100/8 = 12.5，确保 speed×lifetime=range
                keepVelocity = true;
                collides = true;
                collidesTiles = false;
                pierce = false;
                // TODO: replace with LaserBulletType / beam effect
            }}
        );
    }
}
