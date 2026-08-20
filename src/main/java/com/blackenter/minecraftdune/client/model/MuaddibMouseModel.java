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

/** Runtime adaptation of blockbench/java/muaddib_mouse.java. */
public final class MuaddibMouseModel extends HierarchicalModel<MuaddibMouseEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MinecraftDune.MOD_ID, "muaddib_mouse"),
            "main"
    );

    private final ModelPart body;
    private final ModelPart hindlegs;
    private final ModelPart hindRightLeg;
    private final ModelPart hindRightFoot;
    private final ModelPart hindLeftLeg;
    private final ModelPart hindLeftPaw;
    private final ModelPart head;
    private final ModelPart forelegs;
    private final ModelPart tailBase;
    private final ModelPart tailMiddle;
    private final ModelPart tailTip;

    public MuaddibMouseModel(ModelPart root) {
        body = root.getChild("body");
        hindlegs = body.getChild("hindlegs");
        hindRightLeg = hindlegs.getChild("hind_right_leg");
        hindRightFoot = hindRightLeg.getChild("hind_right_foot");
        hindLeftLeg = hindlegs.getChild("hind_left_leg");
        hindLeftPaw = hindLeftLeg.getChild("hind_left_paw");
        head = body.getChild("head");
        forelegs = body.getChild("forelegs");
        tailBase = body.getChild("tail_base");
        tailMiddle = tailBase.getChild("middle");
        tailTip = tailMiddle.getChild("tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(2, 6)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(12, 4)
                        .addBox(-1.0F, 0.0F, -2.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                // The Blockbench export's head points along +Z. Minecraft entity models face
                // -Z, so rotate the complete hierarchy once instead of changing every cube.
                PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, (float) Math.PI, 0.0F)
        );

        PartDefinition hindlegs = body.addOrReplaceChild(
                "hindlegs",
                CubeListBuilder.create(),
                PartPose.offset(-1.0F, 1.0F, -1.5F)
        );
        PartDefinition hindRightLeg = hindlegs.addOrReplaceChild(
                "hind_right_leg",
                CubeListBuilder.create(),
                PartPose.offset(2.0F, 0.0F, 0.0F)
        );
        hindRightLeg.addOrReplaceChild(
                "hind_right_stfile_cube_r1",
                CubeListBuilder.create().texOffs(8, 0)
                        .addBox(-0.5F, -0.5858F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        hindRightLeg.addOrReplaceChild(
                "hind_right_foot",
                CubeListBuilder.create().texOffs(4, 4)
                        .addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.0F, -1.5F)
        );

        PartDefinition hindLeftLeg = hindlegs.addOrReplaceChild(
                "hind_left_leg",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        hindLeftLeg.addOrReplaceChild(
                "hind_left_stfile_cube_r1",
                CubeListBuilder.create().texOffs(8, 0).mirror()
                        .addBox(-0.5F, -0.5858F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F)
        );
        hindLeftLeg.addOrReplaceChild(
                "hind_left_paw",
                CubeListBuilder.create().texOffs(4, 4).mirror()
                        .addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offset(0.0F, 2.0F, -1.5F)
        );

        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(2, 2)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 1.0F)
        );
        PartDefinition leftEar = head.addOrReplaceChild(
                "left_ear",
                CubeListBuilder.create(),
                PartPose.offset(-0.5F, -0.0653F, 1.0043F)
        );
        leftEar.addOrReplaceChild(
                "left_ear_cube_r1",
                CubeListBuilder.create().texOffs(0, 0).mirror()
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, -0.4347F, -0.5043F, 0.0F, 0.0F, -0.5236F)
        );
        PartDefinition rightEar = head.addOrReplaceChild(
                "right_ear",
                CubeListBuilder.create(),
                PartPose.offset(0.5F, -0.0653F, 1.0043F)
        );
        rightEar.addOrReplaceChild(
                "right_ear_cube_r1",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.4347F, -0.5043F, 0.0F, 0.0F, 0.5236F)
        );
        head.addOrReplaceChild(
                "nose",
                CubeListBuilder.create().texOffs(0, 6)
                        .addBox(-4.5F, -0.4347F, 2.4957F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, -0.0653F, -0.9957F)
        );

        PartDefinition forelegs = body.addOrReplaceChild(
                "forelegs",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.5F, 0.5F)
        );
        PartDefinition leftForePaw = forelegs.addOrReplaceChild(
                "left_fore_paw",
                CubeListBuilder.create(),
                PartPose.offset(-1.0F, 0.0F, 0.0F)
        );
        leftForePaw.addOrReplaceChild(
                "left_fore_paw_cube_r1",
                CubeListBuilder.create().texOffs(13, 6).mirror()
                        .addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .mirror(false),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F)
        );
        PartDefinition rightForePaw = forelegs.addOrReplaceChild(
                "right_fore_paw",
                CubeListBuilder.create(),
                PartPose.offset(1.0F, 0.0F, 0.0F)
        );
        rightForePaw.addOrReplaceChild(
                "right_fore_paw_cube_r1",
                CubeListBuilder.create().texOffs(13, 6)
                        .addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F)
        );

        PartDefinition tailBase = body.addOrReplaceChild(
                "tail_base",
                CubeListBuilder.create().texOffs(0, 10)
                        .addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.5F, -2.0F)
        );
        PartDefinition middle = tailBase.addOrReplaceChild(
                "middle",
                CubeListBuilder.create().texOffs(0, 10)
                        .addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, -2.0F)
        );
        middle.addOrReplaceChild(
                "tip",
                CubeListBuilder.create().texOffs(12, 1)
                        .addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, -2.0F)
        );

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return body;
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
        body.getAllParts().forEach(ModelPart::resetPose);

        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float hop = Mth.sin(limbSwing * 0.85F) * movement;
        float lift = Math.abs(hop);

        body.y -= lift * 0.65F;
        hindlegs.xRot += hop * 0.70F;
        hindRightLeg.xRot -= hop * 0.34F;
        hindLeftLeg.xRot -= hop * 0.34F;
        hindRightFoot.xRot += hop * 0.48F;
        hindLeftPaw.xRot += hop * 0.48F;
        forelegs.xRot -= hop * 0.42F;
        tailBase.xRot += hop * 0.14F;
        tailMiddle.xRot += hop * 0.10F;
        tailTip.xRot += hop * 0.07F;

        float idleTail = Mth.sin(ageInTicks * 0.08F) * (1.0F - movement) * 0.08F;
        tailBase.yRot += idleTail;
        tailMiddle.yRot += idleTail * 1.25F;
        tailTip.yRot += idleTail * 1.45F;

        head.yRot += Mth.clamp(netHeadYaw, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
        // The 180-degree body-facing correction reverses the head bone's local X axis.
        // Negate pitch so Minecraft's look-up/look-down input remains visually correct.
        head.xRot -= Mth.clamp(headPitch, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
    }
}
