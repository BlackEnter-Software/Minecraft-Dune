package com.blackenter.minecraftdune.client.model;

import com.blackenter.minecraftdune.MinecraftDune;
import com.blackenter.minecraftdune.entity.MuaddibMouseEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Runtime model generated from blockbench/muaddib_mouse.bbmodel.
 *
 * The Blockbench file remains the editable geometry source. Animation definitions
 * live in MuaddibMouseAnimations and are applied to these named bones.
 */
public final class MuaddibMouseModel extends HierarchicalModel<MuaddibMouseEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MinecraftDune.MOD_ID, "muaddib_mouse"),
            "main"
    );

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart left_ear;
    private final ModelPart right_ear;
    private final ModelPart tail_base;
    private final ModelPart tail_middle;
    private final ModelPart tail_tip;
    private final ModelPart left_hind_rump;
    private final ModelPart right_hind_rump;
    private final ModelPart left_hind_stifle;
    private final ModelPart right_hinde_stifle;
    private final ModelPart left_hind_feet;
    private final ModelPart right_hind_feet;
    private final ModelPart front_leg;
    private final ModelPart left_front_leg;
    private final ModelPart right_front_leg;

    public MuaddibMouseModel(ModelPart bakedRoot) {
        this.root = bakedRoot.getChild("root");
        this.body = bakedRoot.getChild("root").getChild("body");
        this.head = bakedRoot.getChild("root").getChild("body").getChild("head");
        this.left_ear = bakedRoot.getChild("root").getChild("body").getChild("head").getChild("left_ear");
        this.right_ear = bakedRoot.getChild("root").getChild("body").getChild("head").getChild("right_ear");
        this.tail_base = bakedRoot.getChild("root").getChild("body").getChild("tail_base");
        this.tail_middle = bakedRoot.getChild("root").getChild("body").getChild("tail_base").getChild("tail_middle");
        this.tail_tip = bakedRoot.getChild("root").getChild("body").getChild("tail_base").getChild("tail_middle").getChild("tail_tip");
        this.left_hind_rump = bakedRoot.getChild("root").getChild("left_hind_rump");
        this.right_hind_rump = bakedRoot.getChild("root").getChild("right_hind_rump");
        this.left_hind_stifle = bakedRoot.getChild("root").getChild("left_hind_rump").getChild("left_hind_stifle");
        this.right_hinde_stifle = bakedRoot.getChild("root").getChild("right_hind_rump").getChild("right_hinde_stifle");
        this.left_hind_feet = bakedRoot.getChild("root").getChild("left_hind_rump").getChild("left_hind_stifle").getChild("left_hind_feet");
        this.right_hind_feet = bakedRoot.getChild("root").getChild("right_hind_rump").getChild("right_hinde_stifle").getChild("right_hind_feet");
        this.front_leg = bakedRoot.getChild("root").getChild("body").getChild("front_leg");
        this.left_front_leg = bakedRoot.getChild("root").getChild("body").getChild("front_leg").getChild("left_front_leg");
        this.right_front_leg = bakedRoot.getChild("root").getChild("body").getChild("front_leg").getChild("right_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 17.0F, 0.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition lower_body_cube_r1 = body.addOrReplaceChild("lower_body_cube_r1", CubeListBuilder.create().texOffs(38, 0).addBox(-1.5F, -3.0F, -4.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition upper_body_cube_r1 = body.addOrReplaceChild("upper_body_cube_r1", CubeListBuilder.create().texOffs(17, 0).addBox(-2.5F, -3.0F, -4.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -3.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 1).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(13, 0).addBox(-1.0F, -0.5F, -4.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -5.25F, -7.0F));

		PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.25F, -1.5F, -0.5F, 0.1222F, 0.0F, 0.1833F));

		PartDefinition left_ear_cube_r1 = left_ear.addOrReplaceChild("left_ear_cube_r1", CubeListBuilder.create().texOffs(0, 10).addBox(-1.5F, -6.0F, -0.5F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5236F));

		PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offsetAndRotation(1.25F, -1.5F, -0.5F, 0.1222F, 0.0F, -0.1833F));

		PartDefinition right_ear_cube_r1 = right_ear.addOrReplaceChild("right_ear_cube_r1", CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-1.5F, -6.0F, -0.5F, 3.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5236F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create().texOffs(9, 10).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.5F, 3.0F));

		PartDefinition tail_middle = tail_base.addOrReplaceChild("tail_middle", CubeListBuilder.create().texOffs(18, 11).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 3.0F));

		PartDefinition tail_tip = tail_middle.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(25, 10).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 2.0F));

		PartDefinition front_leg = body.addOrReplaceChild("front_leg", CubeListBuilder.create(), PartPose.offset(0.0F, 7.0F, -3.0F));

		PartDefinition left_front_leg = front_leg.addOrReplaceChild("left_front_leg", CubeListBuilder.create(), PartPose.offset(-2.0F, -10.0F, -1.5F));

		PartDefinition left_front_leg_cube_r1 = left_front_leg.addOrReplaceChild("left_front_leg_cube_r1", CubeListBuilder.create().texOffs(33, 0).addBox(-0.25F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 1.5F, -1.0F, -0.6545F, 0.0F, 0.0F));

		PartDefinition right_front_leg = front_leg.addOrReplaceChild("right_front_leg", CubeListBuilder.create(), PartPose.offset(2.0F, -10.0F, -1.5F));

		PartDefinition right_front_leg_cube_r1 = right_front_leg.addOrReplaceChild("right_front_leg_cube_r1", CubeListBuilder.create().texOffs(33, 0).mirror().addBox(-6.25F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(6.25F, 1.5F, -1.0F, -0.6545F, 0.0F, 0.0F));

		PartDefinition left_hind_rump = root.addOrReplaceChild("left_hind_rump", CubeListBuilder.create().texOffs(0, 18).addBox(-0.75F, -1.5846F, -2.6168F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.25F, -0.5F, 4.0F, -0.1222F, 0.0F, 0.0F));

		PartDefinition left_hind_stifle = left_hind_rump.addOrReplaceChild("left_hind_stifle", CubeListBuilder.create(), PartPose.offset(0.25F, 2.5F, -2.0F));

		PartDefinition left_hind_stifle_cube_r1 = left_hind_stifle.addOrReplaceChild("left_hind_stifle_cube_r1", CubeListBuilder.create().texOffs(13, 17).addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.5F, 2.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition left_hind_feet = left_hind_stifle.addOrReplaceChild("left_hind_feet", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 3.0F));

		PartDefinition left_hind_feet_cube_r1 = left_hind_feet.addOrReplaceChild("left_hind_feet_cube_r1", CubeListBuilder.create().texOffs(28, 17).addBox(-3.0F, -0.5F, -2.5F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 1.5F, -1.0F, 0.1309F, 0.0F, 0.0F));

		PartDefinition right_hind_rump = root.addOrReplaceChild("right_hind_rump", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-1.25F, -1.5846F, -2.6168F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(2.25F, -0.5F, 4.0F, -0.1222F, 0.0F, 0.0F));

		PartDefinition right_hinde_stifle = right_hind_rump.addOrReplaceChild("right_hinde_stifle", CubeListBuilder.create(), PartPose.offset(-0.25F, 2.5F, -2.0F));

		PartDefinition right_hind_stifle_cube_r1 = right_hinde_stifle.addOrReplaceChild("right_hind_stifle_cube_r1", CubeListBuilder.create().texOffs(13, 17).mirror().addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 1.5F, 2.0F, -0.6109F, 0.0F, 0.0F));

		PartDefinition right_hind_feet = right_hinde_stifle.addOrReplaceChild("right_hind_feet", CubeListBuilder.create(), PartPose.offset(0.0F, 2.0F, 3.0F));

		PartDefinition right_hind_feet_cube_r1 = right_hind_feet.addOrReplaceChild("right_hind_feet_cube_r1", CubeListBuilder.create().texOffs(28, 17).mirror().addBox(1.0F, -0.5F, -2.5F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-2.0F, 1.5F, -1.0F, 0.1309F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 32);
	}
    /** ChatGPT
		MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();

        PartDefinition rootPart = meshRoot.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 17.0F, 0.0F)
        );
        PartDefinition bodyPart = rootPart.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 3.0F)
        );
        bodyPart.addOrReplaceChild(
                "upper_body_cube",
                CubeListBuilder.create().texOffs(17, 0)
                                .addBox(-2.5F, -3.0F, -4.0F, 5.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -3.0F, radians(35.0F), radians(0.0F), radians(0.0F))
        );
        bodyPart.addOrReplaceChild(
                "lower_body_cube",
                CubeListBuilder.create().texOffs(38, 0)
                                .addBox(-1.5F, -3.0F, -4.0F, 3.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 1.0F, 1.0F, radians(35.0F), radians(0.0F), radians(0.0F))
        );
        PartDefinition headPart = bodyPart.addOrReplaceChild(
                "head",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -5.25F, -7.0F)
        );
        headPart.addOrReplaceChild(
                "head_cube",
                CubeListBuilder.create().texOffs(0, 1)
                                .addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        headPart.addOrReplaceChild(
                "muzzle",
                CubeListBuilder.create().texOffs(13, 0)
                                .addBox(-1.0F, -0.5F, -4.5F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition left_earPart = headPart.addOrReplaceChild(
                "left_ear",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.25F, -1.5F, -0.5F, radians(-7.0F), radians(0.0F), radians(10.5F))
        );
        left_earPart.addOrReplaceChild(
                "left_ear_cube",
                CubeListBuilder.create().texOffs(0, 10)
                                .addBox(-1.5F, -6.0F, -0.5F, 3.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, radians(0.0F), radians(0.0F), radians(-30.0F))
        );
        PartDefinition right_earPart = headPart.addOrReplaceChild(
                "right_ear",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.25F, -1.5F, -0.5F, radians(-7.0F), radians(0.0F), radians(-10.5F))
        );
        right_earPart.addOrReplaceChild(
                "right_ear_cube",
                CubeListBuilder.create().texOffs(0, 10)
                                .mirror()
                                .addBox(-1.5F, -6.0F, -0.5F, 3.0F, 6.0F, 1.0F)
                                .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, radians(0.0F), radians(0.0F), radians(30.0F))
        );
        PartDefinition tail_basePart = bodyPart.addOrReplaceChild(
                "tail_base",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, -0.5F, 3.0F)
        );
        tail_basePart.addOrReplaceChild(
                "tail_base_cube",
                CubeListBuilder.create().texOffs(9, 10)
                                .addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F)
        );
        PartDefinition tail_middlePart = tail_basePart.addOrReplaceChild(
                "tail_middle",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 3.0F)
        );
        tail_middlePart.addOrReplaceChild(
                "tail_middle_cube",
                CubeListBuilder.create().texOffs(18, 11)
                                .addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F)
        );
        PartDefinition tail_tipPart = tail_middlePart.addOrReplaceChild(
                "tail_tip",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 2.0F)
        );
        tail_tipPart.addOrReplaceChild(
                "tail_tip_cube",
                CubeListBuilder.create().texOffs(25, 10)
                                .addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F)
        );
        PartDefinition front_legPart = bodyPart.addOrReplaceChild(
                "front_leg",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 7.0F, -3.0F)
        );
        PartDefinition left_front_legPart = front_legPart.addOrReplaceChild(
                "left_front_leg",
                CubeListBuilder.create(),
                PartPose.offset(2.0F, -10.0F, -1.5F)
        );
        left_front_legPart.addOrReplaceChild(
                "left_front_leg_cube",
                CubeListBuilder.create().texOffs(33, 0)
                                .addBox(-0.75F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.75F, 1.5F, -1.0F, radians(37.5F), radians(0.0F), radians(0.0F))
        );
        PartDefinition right_front_legPart = front_legPart.addOrReplaceChild(
                "right_front_leg",
                CubeListBuilder.create(),
                PartPose.offset(-2.0F, -10.0F, -1.5F)
        );
        right_front_legPart.addOrReplaceChild(
                "right_front_leg_cube",
                CubeListBuilder.create().texOffs(33, 0)
                                .mirror()
                                .addBox(5.25F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F)
                                .mirror(false),
                PartPose.offsetAndRotation(-6.25F, 1.5F, -1.0F, radians(37.5F), radians(0.0F), radians(0.0F))
        );
        PartDefinition left_hind_rumpPart = rootPart.addOrReplaceChild(
                "left_hind_rump",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.25F, -0.5F, 4.0F, radians(7.0F), radians(0.0F), radians(0.0F))
        );
        left_hind_rumpPart.addOrReplaceChild(
                "left_hind_rump_cube",
                CubeListBuilder.create().texOffs(0, 18)
                                .addBox(1.0F, -2.0F, -2.5F, 2.0F, 4.0F, 4.0F),
                PartPose.offset(-2.25F, 0.4154F, -0.1168F)
        );
        PartDefinition left_hind_stiflePart = left_hind_rumpPart.addOrReplaceChild(
                "left_hind_stifle",
                CubeListBuilder.create(),
                PartPose.offset(-0.25F, 2.5F, -2.0F)
        );
        left_hind_stiflePart.addOrReplaceChild(
                "left_hind_stifle_cube",
                CubeListBuilder.create().texOffs(13, 17)
                                .addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 1.5F, 2.0F, radians(35.0F), radians(0.0F), radians(0.0F))
        );
        PartDefinition left_hind_feetPart = left_hind_stiflePart.addOrReplaceChild(
                "left_hind_feet",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 2.0F, 3.0F)
        );
        left_hind_feetPart.addOrReplaceChild(
                "left_hind_feet_cube",
                CubeListBuilder.create().texOffs(28, 17)
                                .addBox(1.0F, -0.5F, -2.5F, 2.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-2.0F, 1.5F, -1.0F, radians(-7.5F), radians(0.0F), radians(0.0F))
        );
        PartDefinition right_hind_rumpPart = rootPart.addOrReplaceChild(
                "right_hind_rump",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.25F, -0.5F, 4.0F, radians(7.0F), radians(0.0F), radians(0.0F))
        );
        right_hind_rumpPart.addOrReplaceChild(
                "right_hind_rump_cube",
                CubeListBuilder.create().texOffs(0, 18)
                                .mirror()
                                .addBox(-3.0F, -2.0F, -2.5F, 2.0F, 4.0F, 4.0F)
                                .mirror(false),
                PartPose.offset(2.25F, 0.4154F, -0.1168F)
        );
        PartDefinition right_hinde_stiflePart = right_hind_rumpPart.addOrReplaceChild(
                "right_hinde_stifle",
                CubeListBuilder.create(),
                PartPose.offset(0.25F, 2.5F, -2.0F)
        );
        right_hinde_stiflePart.addOrReplaceChild(
                "right_hind_stifle_cube",
                CubeListBuilder.create().texOffs(13, 17)
                                .mirror()
                                .addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 5.0F)
                                .mirror(false),
                PartPose.offsetAndRotation(0.0F, 1.5F, 2.0F, radians(35.0F), radians(0.0F), radians(0.0F))
        );
        PartDefinition right_hind_feetPart = right_hinde_stiflePart.addOrReplaceChild(
                "right_hind_feet",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 2.0F, 3.0F)
        );
        right_hind_feetPart.addOrReplaceChild(
                "right_hind_feet_cube",
                CubeListBuilder.create().texOffs(28, 17)
                                .mirror()
                                .addBox(-3.0F, -0.5F, -2.5F, 2.0F, 1.0F, 5.0F)
                                .mirror(false),
                PartPose.offsetAndRotation(2.0F, 1.5F, -1.0F, radians(-7.5F), radians(0.0F), radians(0.0F))
        );

        return LayerDefinition.create(mesh, 64, 32);
    } */

    private static float radians(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(
            MuaddibMouseEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        root.getAllParts().forEach(ModelPart::resetPose);

        // Continuous subtle posture motion while standing.
        animate(entity.idleAnimationState, MuaddibMouseAnimations.IDLE, ageInTicks);

        // A coordinated kangaroo-mouse hop. Both rear legs work together rather
        // than alternating as ordinary walking legs would.
        animateWalk(
                MuaddibMouseAnimations.HOP,
                limbSwing,
                limbSwingAmount,
                2.0F,
                2.5F
        );

        // Preserve the two actions authored in the uploaded Blockbench model.
        animate(entity.sniffAnimationState, MuaddibMouseAnimations.SNIFF_GROUND, ageInTicks);
        animate(entity.wiggleHeadAnimationState, MuaddibMouseAnimations.WIGGLE_HEAD, ageInTicks);

        float lookScale = entity.sniffAnimationState.isStarted() ? 0.20F : 1.0F;
        head.yRot += Mth.clamp(netHeadYaw, -45.0F, 45.0F) * Mth.DEG_TO_RAD * lookScale;
        head.xRot += Mth.clamp(headPitch, -30.0F, 30.0F) * Mth.DEG_TO_RAD * lookScale;
    }
}
