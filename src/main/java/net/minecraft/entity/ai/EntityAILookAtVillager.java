package net.minecraft.entity.ai;


import net.canarymod.api.ai.CanaryAILookAtVillager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.passive.EntityVillager;


public class EntityAILookAtVillager extends EntityAIBase {

    private EntityIronGolem a;
    private EntityVillager b;
    private int c;
   
    public EntityAILookAtVillager(EntityIronGolem entityirongolem) {
        this.a = entityirongolem;
        this.a(3);
        this.canaryAI = new CanaryAILookAtVillager(this); //CanaryMod: set our variable
    }

    public boolean a() {
        if (!this.a.o.w()) {
            return false;
        } else if (this.a.bb().nextInt(8000) != 0) {
            return false;
        } else {
            this.b = (EntityVillager) this.a.o.a(EntityVillager.class, this.a.aQ().b(6.0D, 2.0D, 6.0D), (Entity) this.a);
            return this.b != null;
        }
    }

    public boolean b() {
        return this.c > 0;
    }

    public void c() {
        this.c = 400;
        this.a.a(true);
    }

    public void d() {
        this.a.a(false);
        this.b = null;
    }

    public void e() {
        this.a.p().a(this.b, 30.0F, 30.0F);
        --this.c;
    }
}
