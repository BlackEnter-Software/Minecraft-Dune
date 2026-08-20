// Made with Blockbench 5.1.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class muaddib_mouse<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "muaddib_mouse"), "main");
	private final ModelPart body;
	private final ModelPart hindlegs;
	private final ModelPart hind_right_leg;
	private final ModelPart hind_right_foot;
	private final ModelPart hind_left_leg;
	private final ModelPart hind_left_paw;
	private final ModelPart head;
	private final ModelPart left_ear;
	private final ModelPart right_ear;
	private final ModelPart nose;
	private final ModelPart forelegs;
	private final ModelPart left_fore_paw;
	private final ModelPart right_fore_paw;
	private final ModelPart tail_base;
	private final ModelPart middle;
	private final ModelPart tip;

	public muaddib_mouse(ModelPart root) {
		this.body = root.getChild("body");
		this.hindlegs = this.body.getChild("hindlegs");
		this.hind_right_leg = this.hindlegs.getChild("hind_right_leg");
		this.hind_right_foot = this.hind_right_leg.getChild("hind_right_foot");
		this.hind_left_leg = this.hindlegs.getChild("hind_left_leg");
		this.hind_left_paw = this.hind_left_leg.getChild("hind_left_paw");
		this.head = this.body.getChild("head");
		this.left_ear = this.head.getChild("left_ear");
		this.right_ear = this.head.getChild("right_ear");
		this.nose = this.head.getChild("nose");
		this.forelegs = this.body.getChild("forelegs");
		this.left_fore_paw = this.forelegs.getChild("left_fore_paw");
		this.right_fore_paw = this.forelegs.getChild("right_fore_paw");
		this.tail_base = this.body.getChild("tail_base");
		this.middle = this.tail_base.getChild("middle");
		this.tip = this.middle.getChild("tip");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(12, 4).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 20.0F, 0.0F));

		PartDefinition hindlegs = body.addOrReplaceChild("hindlegs", CubeListBuilder.create(), PartPose.offset(-1.0F, 1.0F, -1.5F));

		PartDefinition hind_right_leg = hindlegs.addOrReplaceChild("hind_right_leg", CubeListBuilder.create(), PartPose.offset(2.0F, 0.0F, 0.0F));

		PartDefinition hind_right_stfile_cube_r1 = hind_right_leg.addOrReplaceChild("hind_right_stfile_cube_r1", CubeListBuilder.create().texOffs(8, 0).addBox(-0.5F, -0.5858F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition hind_right_foot = hind_right_leg.addOrReplaceChild("hind_right_foot", CubeListBuilder.create().texOffs(4, 4).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 2.0F, -1.5F));

		PartDefinition hind_left_leg = hindlegs.addOrReplaceChild("hind_left_leg", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition hind_left_stfile_cube_r1 = hind_left_leg.addOrReplaceChild("hind_left_stfile_cube_r1", CubeListBuilder.create().texOffs(8, 0).mirror().addBox(-0.5F, -0.5858F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition hind_left_paw = hind_left_leg.addOrReplaceChild("hind_left_paw", CubeListBuilder.create().texOffs(4, 4).mirror().addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 2.0F, -1.5F));

		PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 2).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 1.0F));

		PartDefinition left_ear = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(-0.5F, -0.0653F, 1.0043F));

		PartDefinition left_ear_cube_r1 = left_ear.addOrReplaceChild("left_ear_cube_r1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, -0.4347F, -0.5043F, 0.0F, 0.0F, -0.5236F));

		PartDefinition right_ear = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(0.5F, -0.0653F, 1.0043F));

		PartDefinition right_ear_cube_r1 = right_ear.addOrReplaceChild("right_ear_cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.4347F, -0.5043F, 0.0F, 0.0F, 0.5236F));

		PartDefinition nose = head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(0, 6).addBox(-4.5F, -0.4347F, 2.4957F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(4.0F, -0.0653F, -0.9957F));

		PartDefinition forelegs = body.addOrReplaceChild("forelegs", CubeListBuilder.create(), PartPose.offset(0.0F, 0.5F, 0.5F));

		PartDefinition left_fore_paw = forelegs.addOrReplaceChild("left_fore_paw", CubeListBuilder.create(), PartPose.offset(-1.0F, 0.0F, 0.0F));

		PartDefinition left_fore_paw_cube_r1 = left_fore_paw.addOrReplaceChild("left_fore_paw_cube_r1", CubeListBuilder.create().texOffs(13, 6).mirror().addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

		PartDefinition right_fore_paw = forelegs.addOrReplaceChild("right_fore_paw", CubeListBuilder.create(), PartPose.offset(1.0F, 0.0F, 0.0F));

		PartDefinition right_fore_paw_cube_r1 = right_fore_paw.addOrReplaceChild("right_fore_paw_cube_r1", CubeListBuilder.create().texOffs(13, 6).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

		PartDefinition tail_base = body.addOrReplaceChild("tail_base", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.5F, -2.0F));

		PartDefinition middle = tail_base.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -2.0F));

		PartDefinition tip = middle.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(12, 1).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -2.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}