package endfieldindustrylib.EFworld.blocks.AICTurret;

//全向声波塔
import arc.graphics.Color;
import endfieldindustrylib.EFcontents.EFitems;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Sounds;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.blocks.defense.turrets.ItemTurret;

public class OmnidirectionalSonicTower extends ItemTurret {
    public OmnidirectionalSonicTower(String name) {
        super(name);

        requirements(Category.turret, ItemStack.with(EFitems.origocrust, 20));

        consumePower(20f);

        size = 2;
        range = 48f;        // 6m × 8 = 48
        reload = 300f;      // 5s
        rotateSpeed = 4f;
        inaccuracy = 0f;
        maxAmmo = 15;
        ammoPerShot = 1;
        shootSound = Sounds.shoot;

        targetAir = true;
        targetGround = true;

        ammo(
            EFitems.origocrust, new BasicBulletType(8f, 0) {{ // 速度改为 1格/帧（8f）
                splashDamage = 0f;
                splashDamageRadius = 48f; // 6m splash (6 × 8 = 48)
                // TODO: add stun effect
                width = 14f;
                height = 14f;
                lifetime = 6f; // 48/8 = 6，与 range 匹配
                keepVelocity = true;
                collides = true;
                collidesTiles = false;
                pierce = false;
            }},
            EFitems.amethystFiber, new BasicBulletType(8f, 0) {{ // 速度改为 1格/帧（8f）
                splashDamage = 0f;
                splashDamageRadius = 48f; // 6m splash (6 × 8 = 48)
                // TODO: add stun effect
                width = 14f;
                height = 14f;
                lifetime = 6f; // 48/8 = 6，与 range 匹配
                keepVelocity = true;
                collides = true;
                collidesTiles = false;
                pierce = false;
            }}
        );
    }
}
