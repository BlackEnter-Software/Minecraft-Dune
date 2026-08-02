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

public final class MuaddibMouseModel
        extends HierarchicalModel<MuaddibMouseEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            MinecraftDune.MOD_ID,
                            "muaddib_mouse"
                    ),
                    "main"
            );

    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart tailBase;
    private final ModelPart tailTip;

    public MuaddibMouseModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftEar = head.getChild("left_ear");
        this.rightEar = head.getChild("right_ear");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.tailBase = body.getChild("tail_base");
        this.tailTip = tailBase.getChild("tail_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -2.5F, -3.0F, -4.0F,
                                5.0F, 5.0F, 8.0F,
                                new CubeDeformation(0.0F)
                        ),
                PartPose.offset(0.0F, 20.0F, 0.0F)
        );

        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(
                                -2.0F, -2.0F, -3.5F,
                                4.0F, 4.0F, 4.5F
                        )
                        .texOffs(17, 13)
                        .addBox(
                                -1.0F, -0.5F, -4.5F,
                                2.0F, 2.0F, 1.5F
                        ),
                PartPose.offset(0.0F, -1.25F, -4.0F)
        );

        head.addOrReplaceChild(
                "left_ear",
                CubeListBuilder.create()
                        .texOffs(20, 0)
                        .addBox(
                                -0.5F, -4.0F, -0.5F,
                                2.0F, 4.0F, 1.0F
                        ),
                PartPose.offsetAndRotation(
                        1.25F, -1.5F, -0.5F,
                        -7.0F * DEG_TO_RAD,
                        0.0F,
                        10.5F * DEG_TO_RAD
                )
        );

        head.addOrReplaceChild(
                "right_ear",
                CubeListBuilder.create()
                        .texOffs(20, 0)
                        .mirror()
                        .addBox(
                                -1.5F, -4.0F, -0.5F,
                                2.0F, 4.0F, 1.0F
                        )
                        .mirror(false),
                PartPose.offsetAndRotation(
                        -1.25F, -1.5F, -0.5F,
                        -7.0F * DEG_TO_RAD,
                        0.0F,
                        -10.5F * DEG_TO_RAD
                )
        );

        root.addOrReplaceChild(
                "left_hind_leg",
                CubeListBuilder.create()
                        .texOffs(0, 22)
                        .addBox(
                                -1.0F, -1.5F, -1.5F,
                                2.0F, 3.0F, 5.0F
                        ),
                PartPose.offsetAndRotation(
                        2.25F, 21.5F, 2.0F,
                        7.0F * DEG_TO_RAD, 0.0F, 0.0F
                )
        );

        root.addOrReplaceChild(
                "right_hind_leg",
                CubeListBuilder.create()
                        .texOffs(0, 22)
                        .mirror()
                        .addBox(
                                -1.0F, -1.5F, -1.5F,
                                2.0F, 3.0F, 5.0F
                        )
                        .mirror(false),
                PartPose.offsetAndRotation(
                        -2.25F, 21.5F, 2.0F,
                        7.0F * DEG_TO_RAD, 0.0F, 0.0F
                )
        );

        root.addOrReplaceChild(
                "left_front_leg",
                CubeListBuilder.create()
                        .texOffs(14, 22)
                        .addBox(
                                -0.75F, -1.0F, -1.5F,
                                1.5F, 2.0F, 4.0F
                        ),
                PartPose.offset(1.5F, 22.0F, -2.5F)
        );

        root.addOrReplaceChild(
                "right_front_leg",
                CubeListBuilder.create()
                        .texOffs(14, 22)
                        .mirror()
                        .addBox(
                                -0.75F, -1.0F, -1.5F,
                                1.5F, 2.0F, 4.0F
                        )
                        .mirror(false),
                PartPose.offset(-1.5F, 22.0F, -2.5F)
        );

        PartDefinition tailBase = body.addOrReplaceChild(
                "tail_base",
                CubeListBuilder.create()
                        .texOffs(26, 0)
                        .addBox(
                                -0.5F, -0.5F, 0.0F,
                                1.0F, 1.0F, 5.0F
                        ),
                PartPose.offsetAndRotation(
                        0.0F, -0.5F, 3.75F,
                        -10.5F * DEG_TO_RAD, 0.0F, 0.0F
                )
        );

        tailBase.addOrReplaceChild(
                "tail_tip",
                CubeListBuilder.create()
                        .texOffs(26, 7)
                        .addBox(
                                -0.5F, -0.5F, 0.0F,
                                1.0F, 1.0F, 5.0F,
                                new CubeDeformation(-0.08F)
                        ),
                PartPose.offsetAndRotation(
                        0.0F, 0.0F, 4.75F,
                        6.0F * DEG_TO_RAD, 0.0F, 0.0F
                )
        );

        return LayerDefinition.create(mesh, 64, 32);
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

        head.yRot = netHeadYaw * DEG_TO_RAD;
        head.xRot = headPitch * DEG_TO_RAD;

        float movement = Math.min(limbSwingAmount, 1.0F);
        float hop = Mth.sin(limbSwing * 0.9F) * movement;
        float absoluteHop = Math.abs(hop);

        leftHindLeg.xRot += hop * 0.90F;
        rightHindLeg.xRot += hop * 0.90F;
        leftFrontLeg.xRot = -hop * 0.65F;
        rightFrontLeg.xRot = -hop * 0.65F;

        body.y = 20.0F - absoluteHop * 0.75F;
        body.xRot = -0.05F + absoluteHop * 0.12F;

        float earTwitch = Mth.sin(ageInTicks * 0.18F) * 0.035F;
        leftEar.zRot = 10.5F * DEG_TO_RAD + earTwitch;
        rightEar.zRot = -10.5F * DEG_TO_RAD - earTwitch;

        float tailSway = Mth.sin(ageInTicks * 0.12F) * 0.12F;
        tailBase.yRot = tailSway;
        tailTip.yRot = tailSway * 1.35F;
    }
}
