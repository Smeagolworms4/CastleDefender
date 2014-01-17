package mods.castledefenders.common.worldgenerator;

import java.util.ArrayList;
import java.util.Random;

import mods.castledefenders.common.ModCastleDefenders;
import mods.castledefenders.common.building.Building;
import mods.castledefenders.common.building.Building.Unity;
import mods.castledefenders.common.building.Building.Unity.Content;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockWall;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.Direction;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import cpw.mods.fml.common.IWorldGenerator;

public class WorldGeneratorByBuilding implements IWorldGenerator {

	public static final int DIMENSION_ID_NETHER = -1;
	public static final int DIMENSION_ID_SURFACE = 0;
	
	private class BuildingAndInfos {
		Building building;
		int spawnRate;
	}
	
	/**
	 * Spawn global de tous les batiment de cette instance de worldGenerator
	 */
	int globalSpawnRate = 0;

	private ArrayList<BuildingAndInfos> buildingsNether  = new ArrayList<BuildingAndInfos> ();
	private ArrayList<BuildingAndInfos> buildingsSurface = new ArrayList<BuildingAndInfos> ();
	
	
	
	public WorldGeneratorByBuilding(int globalSpawnRate) {
		this.globalSpawnRate = globalSpawnRate;
	}
	

	public void addbuilding(Building building, int buildingSpawnRate) {
		this.addbuilding(building, buildingSpawnRate, this.DIMENSION_ID_SURFACE);
	}
	
	/**
	 * Ajoute un batiment
	 * @param buildingMercenary1
	 * @param mercenaryBuilding1SpawnRate
	 * @param dimensionIdSurface
	 */
	public void addbuilding(Building building, int buildingSpawnRate, int dimensionId) {
		
		BuildingAndInfos buildingAndInfos = new BuildingAndInfos ();
		buildingAndInfos.building         = building;
		buildingAndInfos.spawnRate        = buildingSpawnRate;
		
		switch (dimensionId) {
			case WorldGeneratorByBuilding.DIMENSION_ID_NETHER:
				this.buildingsNether.add(buildingAndInfos);
				break;
				
			case WorldGeneratorByBuilding.DIMENSION_ID_SURFACE:
				this.buildingsSurface.add(buildingAndInfos);
				break;
			default:
		}
	}
	
	/**
	 * Methode de genera
	 */
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		
		// Generation diffenrente entre le nether et la surface
		switch (world.provider.dimensionId) {
			case WorldGeneratorByBuilding.DIMENSION_ID_NETHER:
				this.generateBuilding(world, random, chunkX, chunkZ, buildingsNether);
				break;
				
			case WorldGeneratorByBuilding.DIMENSION_ID_SURFACE:
				this.generateBuilding(world, random, chunkX, chunkZ, buildingsSurface);
				break;
			default:
		}
	}
	
	/**
	 * Genere le batiment dans le terrain correspondant
	 * @param world
	 * @param random
	 * @param wolrdX
	 * @param wolrdZ
	 * @param buildings
	 * @param random
	 */
	private void generateBuilding(World world, Random random, int chunkX, int chunkZ, ArrayList<BuildingAndInfos> buildings) {
		
		if (buildings.size() == 0) {
			return;
		}
		
		// test du Spawn global
		if (random.nextInt(50) < Math.min (this.globalSpawnRate, 10)) { // Pour test sur un superflat // revoie le calcul 1 chance sur 6 par defaut
//		if (random.nextInt(22) < Math.min (this.globalSpawnRate, 10)) {
			

			// Position initial de la génération en hauteur
			int worldY = 64;
			int rotate = random.nextInt(Building.ROTATED_360);
			Building building = this.getBuildingInRate (buildings, random).getRotatetedBuilding (rotate);
			
			// Position initiale du batiment
			int initX = chunkX * 16 + random.nextInt(8) - random.nextInt(8);
			int initY = worldY      + random.nextInt(8) - random.nextInt(8);
			int initZ = chunkZ * 16 + random.nextInt(8) - random.nextInt(8);
			initY = 3; // Pour test sur un superflat
			boolean old = false;
			
			
			//Test si on est sur de la terre (faudrais aps que le batiment vol)
			if (world.getBlockId(initX + 3, initY, initZ + 3) == Block.grass.blockID && !old) {
				
				ModCastleDefenders.log.info("Create building width matrix :"+initX+" "+initY+" "+initZ);
				

				// Parcours la matrice et ajoute des blocks de stone pour les blocks qui s'accroche
				for (int x= 0; x < building.maxX; x++) {
					for (int y= 0; y < building.maxY; y++) {
						for (int z= 0; z < building.maxZ; z++) {
								// Position réél dans le monde du block
								int finalX = initX + x;
								int finalY = initY + y;
								int finalZ = initZ + z;
								world.setBlock(finalX, finalY, finalZ, Block.grass.blockID, 0, 2);
				
						}
					}
				}
				
				// Parcours la matrice et ajoute les blocks
				for (int x= 0; x < building.maxX; x++) {
					for (int y= 0; y < building.maxY; y++) {
						for (int z= 0; z < building.maxZ; z++) {
							
							Unity unity = building.get(x, y, z);
							
							// Position réél dans le monde du block
							int finalX = initX + x;
							int finalY = initY + y;
							int finalZ = initZ + z;
							
							if (unity.block != null) {
								world.setBlock(finalX, finalY, finalZ, unity.block.blockID, unity.metadata, 2);
							} else {
								world.setBlock(finalX, finalY, finalZ, 0, 0, 2);
							}

							this.setOrientation (world, finalX, finalY, finalZ, this.rotateOrientation(rotate, unity.orientation));
							this.setContents    (world, random, finalX, finalY, finalZ, unity.contents);
						}
					}
				}
				
				
				/////////////////////////////////////////////////////////////
				// Rempli en dessous du batiment pour pas que ca sois vide //
				/////////////////////////////////////////////////////////////
				
				int maxYdown = 0;
				
				for (int x= 0; x < building.maxX; x++) {
					for (int z= 0; z < building.maxZ; z++) {
						
						int y = -1;
						int finalX = initX + x;
						int finalY = initY + y;
						int finalZ = initZ + z;
						// recherche la profondeur maxi de Y
						while (
							world.getBlockId(finalX, finalY, finalZ) != Block.grass.blockID &&
							world.getBlockId(finalX, finalY, finalZ) != Block.stone.blockID &&
							world.getBlockId(finalX, finalY, finalZ) != Block.dirt.blockID &&
							world.getBlockId(finalX, finalY, finalZ) != Block.bedrock.blockID &&
							finalY > 0
						){
							
							maxYdown = Math.min (maxYdown, y);
							
							y--;
							finalX = initX + x;
							finalY = initY + y;
							finalZ = initZ + z;
						}
					}
				}
				// Crée un escalier sur les block de remplissage pour que ca sois plus jolie
				for (int y = maxYdown; y < 0; y++) {
					
					// Les escalier sont au minimum de 4
					int yMin = Math.max (y, -3);
					
					for (int x = yMin ; x < building.maxX + (-yMin); x++) {
						for (int z = yMin; z < building.maxZ + (-yMin); z++) {
							int finalX = initX + x;
							int finalY = initY + y;
							int finalZ = initZ + z;
							
							
							// Fait des escalier sans angles
							if (x < 0 && z < 0                           && Math.abs(x)                 + Math.abs(z)                 >= Math.abs(yMin) + 1) { continue; }
							if (x < 0 && z >= building.maxZ              && Math.abs(x)                 + Math.abs(z - building.maxZ) >= Math.abs(yMin))     { continue; }
							if (z < 0 && x >= building.maxX              && Math.abs(z)                 + Math.abs(x - building.maxX) >= Math.abs(yMin))     { continue; }
							if (x >= building.maxX && z >= building.maxZ && Math.abs(x - building.maxX) + Math.abs(z - building.maxZ) >= Math.abs(yMin) - 1) { continue; }
							
							if (
								world.getBlockId(finalX, finalY, finalZ) != Block.grass.blockID &&
								world.getBlockId(finalX, finalY, finalZ) != Block.stone.blockID&&
								world.getBlockId(finalX, finalY, finalZ) != Block.dirt.blockID &&
								world.getBlockId(finalX, finalY, finalZ) != Block.bedrock.blockID &&
								finalY > 0
							) {
								if (
									y > -4
								) {
									world.setBlock(finalX, finalY, finalZ, Block.grass.blockID, 0, 2);
								} else {
									world.setBlock(finalX, finalY, finalZ, Block.stone.blockID, 0, 2);
								}
							}
						}
					}
					
				}
			}
			else {
				this.old (world, random, initX, initY, initZ);
			}
		}
	}
	

    private ItemStack pickCheckLootItem(Random var1)
    {
        return new ItemStack(ModCastleDefenders.itemMedallion);
    }

    private ItemStack pickCheckLootItem3(Random var1)
    {
        int var2 = var1.nextInt(9);
        return var2 == 0 ? new ItemStack(Item.arrow, var1.nextInt(30) + 10) : (var2 == 1 ? new ItemStack(Item.bread) : (var2 == 2 ? new ItemStack(Item.swordIron) : (var2 == 3 ? new ItemStack(Item.emerald) : (var2 == 4 ? new ItemStack(Item.bowlSoup) : (var2 == 5 ? new ItemStack(Item.diamond, var1.nextInt(1) + 1) : (var2 == 6 ? new ItemStack(Item.axeIron) : (var2 == 7 ? new ItemStack(Item.bowlSoup) : (var2 == 8 ? new ItemStack(Item.legsChain) : null))))))));
    }

    private ItemStack pickCheckLootItem2(Random var1)
    {
        int var2 = var1.nextInt(13);
        return var2 == 0 ? new ItemStack(Item.carrot) : (var2 == 1 ? new ItemStack(Item.ingotIron, var1.nextInt(4) + 1) : (var2 == 2 ? new ItemStack(Item.ingotGold, var1.nextInt(4) + 1) : (var2 == 3 ? new ItemStack(Item.compass) : (var2 == 4 ? new ItemStack(Item.gunpowder, var1.nextInt(7) + 1) : (var2 == 5 ? new ItemStack(Item.arrow, var1.nextInt(22) + 8) : (var2 == 6 ? new ItemStack(Item.bucketEmpty) : (var2 == 7 && var1.nextInt(10) == 0 ? new ItemStack(Item.appleGold) : (var2 == 8 && var1.nextInt(2) == 0 ? new ItemStack(Item.redstone, var1.nextInt(9) + 1) : (var2 == 9 && var1.nextInt(10) == 0 ? new ItemStack(Item.helmetChain) : (var2 == 10 ? new ItemStack(Item.egg, var1.nextInt(4) + 1) : (var2 == 11 ? new ItemStack(Item.bootsChain) : (var2 == 12 ? new ItemStack(Item.skull) : null))))))))))));
    }
	private void old(World world, Random random, int initX, int initY, int initZ) {

		World var1 = world;
		Random var2 = random;
		
		
		byte var5 = 64;
        int var6 = var2.nextInt(2);
        int var7;
        int var8;
        int var9;
        int var10;
        int var11;
        int var12;
        int var13;
        int var14;
        ItemStack var17;
        ItemStack var20;

//        if (var2.nextInt(mod_castledef.CastleSpawnRaste) == 0 && var6 == 0)
//        {
//            for (var7 = 0; var7 < 1; ++var7)
//            {
                var8 = initX;
                var9 = initY;
                var10 = initZ;

                    var11 = var2.nextInt(2);

                    for (var12 = var9; var12 < var9 + 8; ++var12)
                    {
                        for (var13 = 0; var13 < 14; ++var13)
                        {
                            for (var14 = 0; var14 < 14; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, Block.cobblestone.blockID);
                            }
                        }
                    }

                    for (var12 = var9 + 1; var12 < var9 + 7; ++var12)
                    {
                        for (var13 = 1; var13 < 13; ++var13)
                        {
                            for (var14 = 1; var14 < 13; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, 0);
                            }
                        }
                    }

                    for (var12 = var9 + 7; var12 < var9 + 8; ++var12)
                    {
                        for (var13 = 3; var13 < 11; ++var13)
                        {
                            for (var14 = 3; var14 < 11; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, 0);
                            }
                        }
                    }

                    for (var12 = var9 + 8; var12 < var9 + 9; ++var12)
                    {
                        for (var13 = 0; var13 < 14; ++var13)
                        {
                            for (var14 = 0; var14 < 14; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, Block.cobblestone.blockID);
                            }
                        }
                    }

                    for (var12 = var9 + 8; var12 < var9 + 9; ++var12)
                    {
                        for (var13 = 1; var13 < 13; ++var13)
                        {
                            for (var14 = 1; var14 < 13; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, 0);
                            }
                        }
                    }

                    var1.setBlock(var8, var9 + 9, var10, Block.cobblestone.blockID);
                    var1.setBlock(var8 + 13, var9 + 9, var10, Block.cobblestone.blockID);
                    var1.setBlock(var8, var9 + 9, var10 + 13, Block.cobblestone.blockID);
                    var1.setBlock(var8 + 13, var9 + 9, var10 + 13, Block.cobblestone.blockID);
                    var1.setBlock(var8 + 12, var9 + 7, var10 + 12, ModCastleDefenders.blockEArcher.blockID);
                    var1.setBlock(var8 + 12, var9 + 7, var10 + 1, ModCastleDefenders.blockEArcher.blockID);
                    var1.setBlock(var8 + 1, var9 + 7, var10 + 12, ModCastleDefenders.blockEArcher.blockID);
                    var1.setBlock(var8 + 1, var9 + 7, var10 + 1, ModCastleDefenders.blockEArcher.blockID);

                    for (var12 = var9; var12 < var9 + 5; ++var12)
                    {
                        for (var13 = 0; var13 < 1; ++var13)
                        {
                            for (var14 = 5; var14 < 9; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, 98);
                            }
                        }
                    }

                    for (var12 = var9 + 1; var12 < var9 + 4; ++var12)
                    {
                        for (var13 = 0; var13 < 1; ++var13)
                        {
                            for (var14 = 6; var14 < 8; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, 0);
                            }
                        }
                    }

                    var1.setBlock(var8, var9 + 3, var10 + 3, 0);
                    var1.setBlock(var8, var9 + 3, var10 + 2, 0);
                    var1.setBlock(var8, var9 + 3, var10 + 10, 0);
                    var1.setBlock(var8, var9 + 3, var10 + 11, 0);
                    var1.setBlock(var8 + 1, var9 + 1, var10 + 2, ModCastleDefenders.blockEArcher.blockID);
                    var1.setBlock(var8 + 1, var9 + 1, var10 + 11, ModCastleDefenders.blockEArcher.blockID);
                    var1.setBlock(var8 + 1, var9 + 1, var10 + 3, Block.cobblestone.blockID);
                    var1.setBlock(var8 + 1, var9 + 1, var10 + 10, Block.cobblestone.blockID);

                    for (var12 = var9; var12 < var9 + 1; ++var12)
                    {
                        for (var13 = 0; var13 < 14; ++var13)
                        {
                            for (var14 = 0; var14 < 14; ++var14)
                            {
                                var1.setBlock(var8 + var13, var12, var10 + var14, 98);
                            }
                        }
                    }
                    var12 = var2.nextInt(4);
                    int var15;
                    
                    /*
                    if (var12 == 0)
                    {
                        for (var13 = var9 + 1; var13 < var9 + 14; ++var13)
                        {
                            for (var14 = 3; var14 < 11; ++var14)
                            {
                                for (var15 = 3; var15 < 11; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, Block.cobblestone.blockID);
                                }
                            }
                        }

                        for (var13 = var9 + 13; var13 < var9 + 14; ++var13)
                        {
                            for (var14 = 2; var14 < 12; ++var14)
                            {
                                for (var15 = 2; var15 < 12; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, Block.cobblestone.blockID);
                                }
                            }
                        }

                        for (var13 = var9 + 13; var13 < var9 + 14; ++var13)
                        {
                            for (var14 = 2; var14 < 12; ++var14)
                            {
                                for (var15 = 2; var15 < 12; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, Block.cobblestone.blockID);
                                }
                            }
                        }

                        for (var13 = var9 + 14; var13 < var9 + 15; ++var13)
                        {
                            for (var14 = 2; var14 < 12; ++var14)
                            {
                                for (var15 = 2; var15 < 12; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, Block.cobblestone.blockID);
                                }
                            }
                        }

                        for (var13 = var9 + 15; var13 < var9 + 16; ++var13)
                        {
                            for (var14 = 2; var14 < 12; ++var14)
                            {
                                for (var15 = 2; var15 < 12; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, Block.cobblestone.blockID);
                                }
                            }
                        }

                        for (var13 = var9 + 15; var13 < var9 + 16; ++var13)
                        {
                            for (var14 = 3; var14 < 11; ++var14)
                            {
                                for (var15 = 3; var15 < 11; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, 0);
                                }
                            }
                        }

                        for (var13 = var9 + 1; var13 < var9 + 13; ++var13)
                        {
                            for (var14 = 4; var14 < 10; ++var14)
                            {
                                for (var15 = 4; var15 < 10; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, 0);
                                }
                            }
                        }

                        for (var13 = var9 + 1; var13 < var9 + 4; ++var13)
                        {
                            for (var14 = 3; var14 < 4; ++var14)
                            {
                                for (var15 = 6; var15 < 8; ++var15)
                                {
                                    var1.setBlock(var8 + var14, var13, var10 + var15, 0);
                                }
                            }
                        }

                        var1.setBlock(var8 + 7, var9 + 15, var10 + 7, ModCastleDefenders.blockEMageID);
                        var1.setBlock(var8 + 2, var9 + 16, var10 + 2, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 11, var9 + 16, var10 + 2, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 2, var9 + 16, var10 + 11, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 11, var9 + 16, var10 + 11, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 5, var9 + 2, var10 + 4, Block.torchWood.blockID);
                        var1.setBlock(var8 + 5, var9 + 2, var10 + 9, Block.torchWood.blockID);
                        boolean var21 = true;

                        for (var13 = 1; var13 < 15; ++var13)
                        {
                            var1.setBlock(var8 + 7, var9 + var13, var10 + 4, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[3]], 2);
                        }

                        var1.setBlock(var8 + 5, var9 + 8, var10 + 3, 0);
                        var1.setBlock(var8 + 5, var9 + 9, var10 + 3, 0);
                        var1.setBlock(var8 + 5, var9 + 7, var10 + 4, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 6, var9 + 7, var10 + 4, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 6, var9 + 7, var10 + 5, Block.cobblestone.blockID);
                        var1.setBlock(var8 + 7, var9 + 7, var10 + 5, Block.cobblestone.blockID);
                    }*/

                    var1.setBlock(var8 + 6, var9 + 1, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.offsetsXForSide[5]], 2);
                    var1.setBlock(var8 + 6, var9 + 2, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[5]], 2);
                    var1.setBlock(var8 + 6, var9 + 3, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[5]], 2);
                    var1.setBlock(var8 + 6, var9 + 4, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[5]], 2);
                    var1.setBlock(var8 + 6, var9 + 5, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[5]], 2);
                    var1.setBlock(var8 + 6, var9 + 6, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[5]], 2);
                    var1.setBlock(var8 + 6, var9 + 7, var10 + 1, Block.ladder.blockID, 1 << Direction.facingToDirection[Facing.oppositeSide[5]], 2);

                    if (var1.getBlockId(var8 - 1, var9, var10 - 1) == Block.grass.blockID || var1.getBlockId(var8 - 1, var9, var10 - 1) == Block.dirt.blockID)
                    {
                        var1.setBlock(var8 - 1, var9 + 1, var10 - 1, ModCastleDefenders.blockEKnight.blockID);
                    }

                    if (var1.getBlockId(var8 + 15, var9, var10 + 15) == Block.grass.blockID || var1.getBlockId(var8 + 15, var9, var10 + 15) == Block.dirt.blockID)
                    {
                        var1.setBlock(var8 + 15, var9 + 1, var10 + 15, ModCastleDefenders.blockEKnight.blockID);
                    }

                    if (var1.getBlockId(var8 - 1, var9, var10 + 15) == Block.grass.blockID || var1.getBlockId(var8 - 1, var9, var10 + 15) == Block.dirt.blockID)
                    {
                        var1.setBlock(var8 - 1, var9 + 1, var10 + 15, ModCastleDefenders.blockEKnight.blockID);
                    }

                    if (var1.getBlockId(var8 + 15, var9, var10 - 1) == Block.grass.blockID || var1.getBlockId(var8 + 15, var9, var10 - 1) == Block.dirt.blockID)
                    {
                        var1.setBlock(var8 + 15, var9 + 1, var10 - 1, ModCastleDefenders.blockEKnight.blockID);
                    }

                    var1.setBlock(var8 + 11, var9 + 1, var10 + 4, ModCastleDefenders.blockEKnight.blockID);
                    var1.setBlock(var8 + 11, var9 + 1, var10 + 9, ModCastleDefenders.blockEKnight.blockID);

                    for (var13 = var9 + 1; var13 < var9 + 2; ++var13)
                    {
                        for (var14 = 12; var14 < 13; ++var14)
                        {
                            for (var15 = 1; var15 < 13; ++var15)
                            {
                                var1.setBlock(var8 + var14, var13, var10 + var15, 5);
                            }
                        }
                    }

                    var1.setBlock(var8 + 12, var9 + 2, var10 + 4, Block.torchWood.blockID);
                    var1.setBlock(var8 + 12, var9 + 2, var10 + 9, Block.torchWood.blockID);
                    var1.setBlock(var8 + 12, var9 + 1, var10 + 6, 54);
                    var1.setBlock(var8 + 12, var9 + 1, var10 + 7, 54);
                    TileEntityChest var19 = (TileEntityChest)var1.getBlockTileEntity(var8 + 12, var9 + 1, var10 + 7);

                    for (var14 = 0; var14 < 4; ++var14)
                    {
                        var17 = this.pickCheckLootItem2(var2);
                        ItemStack var16 = this.pickCheckLootItem3(var2);

                        if (var17 != null)
                        {
                            var19.setInventorySlotContents(var2.nextInt(var19.getSizeInventory()), var17);
                            var19.setInventorySlotContents(var2.nextInt(var19.getSizeInventory()), var16);
                        }
                    }

                    var20 = this.pickCheckLootItem(var2);

                    if (var20 != null)
                    {
                        var19.setInventorySlotContents(var2.nextInt(var19.getSizeInventory()), var20);
                    }

                    var15 = var2.nextInt(4);

                    if (var15 == 1)
                    {
                        var1.setBlock(var8 + 9, var9 + 1, var10 + 10, Block.tnt.blockID);
                        var1.setBlock(var8 + 9, var9 + 2, var10 + 10, Block.tnt.blockID);
                        var1.setBlock(var8 + 10, var9 + 1, var10 + 10, Block.tnt.blockID);
                    }

                    if (var15 == 2)
                    {
                        var1.setBlock(var8 + 9, var9 + 1, var10 + 10, 42);
                        var1.setBlock(var8 + 9, var9 + 1, var10 + 9, 42);
                        var1.setBlock(var8 + 9, var9 + 2, var10 + 9, 42);
                    }

                    if (var15 == 3)
                    {
                        var1.setBlock(var8 + 9, var9 + 1, var10 + 10, 41);
                        var1.setBlock(var8 + 9, var9 + 2, var10 + 10, 41);
                    }

                    if (var15 == 0)
                    {
                        var1.setBlock(var8 + 9, var9 + 1, var10 + 10, 22);
                        var1.setBlock(var8 + 9, var9 + 1, var10 + 11, 22);
                        var1.setBlock(var8 + 9, var9 + 2, var10 + 11, 22);
                    }
                
//            }
//        }
/*
        if (var6 == 1 && var2.nextInt(mod_castledef.CastleSpawnRaste) == 0)
        {
            for (var7 = 0; var7 < 1; ++var7)
            {
                var8 = var3 + var2.nextInt(8) - var2.nextInt(8);
                var9 = var5 + var2.nextInt(8) - var2.nextInt(8);
                var10 = var4 + var2.nextInt(8) - var2.nextInt(8);

                if (var1.getBlockId(var8, var9 - 1, var10) != 0 && var1.getBlockId(var8, var9, var10) == 0 && var1.getBlockId(var8 + 7, var9 - 1, var10 + 1) != 0 && var1.getBlockId(var8 + 7, var9, var10 + 3) != 0)
                {
                    for (var11 = var9; var11 < var9 + 8; ++var11)
                    {
                        for (var12 = -5; var12 < 9; ++var12)
                        {
                            for (var13 = -5; var13 < 9; ++var13)
                            {
                                var1.setBlock(var8 + var12, var11, var10 + var13, 0, 0, 2);
                            }
                        }
                    }

                    for (var11 = var9; var11 < var9 + 1; ++var11)
                    {
                        for (var12 = 4; var12 < 7; ++var12)
                        {
                            for (var13 = 0; var13 < 5; ++var13)
                            {
                                var1.setBlock(var8 + var12, var11, var10 + var13, 35);
                            }
                        }
                    }

                    for (var11 = var9; var11 < var9 + 1; ++var11)
                    {
                        for (var12 = 4; var12 < 7; ++var12)
                        {
                            for (var13 = 1; var13 < 4; ++var13)
                            {
                                var1.setBlock(var8 + var12, var11, var10 + var13, 0);
                            }
                        }
                    }

                    var1.setBlock(var8 + 7, var9, var10 + 1, 35);
                    var1.setBlock(var8 + 7, var9, var10 + 2, 35);
                    var1.setBlock(var8 + 7, var9 + 1, var10 + 2, 35);
                    var1.setBlock(var8 + 7, var9, var10 + 3, 35);
                    var1.setBlock(var8 + 8, var9, var10 + 2, 35);
                    var1.setBlock(var8 + 6, var9 + 2, var10 + 2, 35);
                    var1.setBlock(var8 + 5, var9 + 2, var10 + 2, 35);
                    var1.setBlock(var8 + 4, var9 + 2, var10 + 2, 35);
                    var1.setBlock(var8 + 6, var9 + 1, var10 + 1, 35);
                    var1.setBlock(var8 + 5, var9 + 1, var10 + 1, 35);
                    var1.setBlock(var8 + 4, var9 + 1, var10 + 1, 35);
                    var1.setBlock(var8 + 6, var9 + 1, var10 + 3, 35);
                    var1.setBlock(var8 + 5, var9 + 1, var10 + 3, 35);
                    var1.setBlock(var8 + 4, var9 + 1, var10 + 3, 35);
                    var1.setBlock(var8 + 6, var9 + 1, var10 + 2, 50);
                    var1.setBlock(var8 + 7, var9, var10 + 2, 85);
                    var1.setBlock(var8 + 5, var9, var10 + 1, 85);
                    var1.setBlock(var8 + 5, var9, var10 + 3, 85);

                    for (var11 = var9; var11 < var9 + 8; ++var11)
                    {
                        for (var12 = 0; var12 < 11; ++var12)
                        {
                            for (var13 = 0; var13 < 11; ++var13)
                            {
                                var1.setBlockMetadataWithNotify(var8 + var12, var11, var10 + var13, 14, 35);
                            }
                        }
                    }

                    var1.setBlock(var8 + 3, var9, var10 + 1, 54);
                    var1.setBlock(var8 + 2, var9, var10 + 2, ModCastleDefenders.blockEKnight.blockID);
                    var11 = var2.nextInt(2);

                    if (var11 == 1)
                    {
                        for (var12 = 0; var12 < 4; ++var12)
                        {
                            for (var13 = 0; var13 < 5; ++var13)
                            {
                                var1.setBlock(var8 + var12, var9, var10 + var13, 85);
                            }
                        }

                        for (var12 = 1; var12 < 4; ++var12)
                        {
                            for (var13 = 1; var13 < 4; ++var13)
                            {
                                var1.setBlock(var8 + var12, var9, var10 + var13, 0);
                            }
                        }

                        var1.setBlock(var8, var9, var10 + 2, 0);
                        var1.setBlock(var8 + 3, var9, var10 + 1, 54);
                        var1.setBlock(var8 + 2, var9, var10 + 2, ModCastleDefenders.blockEKnight.blockID);
                    }

                    if (var11 == 0)
                    {
                        for (var12 = var9; var12 < var9 + 8; ++var12)
                        {
                            for (var13 = 0; var13 < -11; ++var13)
                            {
                                for (var14 = 0; var14 < -11; ++var14)
                                {
                                    var1.setBlock(var8 - var13, var12, var10 - var14, 0, 0, 2);
                                }
                            }
                        }

                        for (var12 = var9; var12 < var9 + 1; ++var12)
                        {
                            for (var13 = 4; var13 < 7; ++var13)
                            {
                                for (var14 = 0; var14 < 5; ++var14)
                                {
                                    var1.setBlock(var8 - var13, var12, var10 - var14, 35);
                                }
                            }
                        }

                        for (var12 = var9; var12 < var9 + 1; ++var12)
                        {
                            for (var13 = 4; var13 < 7; ++var13)
                            {
                                for (var14 = 1; var14 < 4; ++var14)
                                {
                                    var1.setBlock(var8 - var13, var12, var10 - var14, 0);
                                }
                            }
                        }

                        var1.setBlock(var8 - 7, var9, var10 - 1, 35);
                        var1.setBlock(var8 - 7, var9, var10 - 2, 35);
                        var1.setBlock(var8 - 7, var9 + 1, var10 - 2, 35);
                        var1.setBlock(var8 - 7, var9, var10 - 3, 35);
                        var1.setBlock(var8 - 8, var9, var10 - 2, 35);
                        var1.setBlock(var8 - 6, var9 + 2, var10 - 2, 35);
                        var1.setBlock(var8 - 5, var9 + 2, var10 - 2, 35);
                        var1.setBlock(var8 - 4, var9 + 2, var10 - 2, 35);
                        var1.setBlock(var8 - 6, var9 + 1, var10 - 1, 35);
                        var1.setBlock(var8 - 5, var9 + 1, var10 - 1, 35);
                        var1.setBlock(var8 - 4, var9 + 1, var10 - 1, 35);
                        var1.setBlock(var8 - 6, var9 + 1, var10 - 3, 35);
                        var1.setBlock(var8 - 5, var9 + 1, var10 - 3, 35);
                        var1.setBlock(var8 - 4, var9 + 1, var10 - 3, 35);
                        var1.setBlock(var8 - 6, var9 + 1, var10 - 2, 50);
                        var1.setBlock(var8 - 7, var9, var10 - 2, 85);
                        var1.setBlock(var8 - 5, var9, var10 - 1, 85);
                        var1.setBlock(var8 - 5, var9, var10 - 3, 85);

                        for (var12 = var9; var12 < var9 + 8; ++var12)
                        {
                            for (var13 = 0; var13 < 11; ++var13)
                            {
                                for (var14 = 0; var14 < 11; ++var14)
                                {
                                    var1.setBlockMetadataWithNotify(var8 - var13, var12, var10 - var14, 14, 35);
                                }
                            }
                        }

                        for (var12 = 1; var12 < 7; ++var12)
                        {
                            for (var13 = 1; var13 < 4; ++var13)
                            {
                                var1.setBlock(var8 - var12, var9 - 1, var10 - var13, 2);
                            }
                        }

                        var1.setBlock(var8, var9, var10 - 2, 0);
                        var1.setBlock(var8 - 2, var9, var10 - 2, ModCastleDefenders.blockEKnight.blockID);
                        var1.setBlock(var8 + 1, var9 - 1, var10 - 2, 87);
                        var1.setBlock(var8 + 2, var9 - 1, var10 - 2, 4);
                        var1.setBlock(var8, var9 - 1, var10 - 2, 4);
                        var1.setBlock(var8 + 1, var9 - 1, var10 - 1, 4);
                        var1.setBlock(var8 + 1, var9 - 1, var10 - 3, 4);
                        var1.setBlock(var8 + 1, var9, var10 - 2, 51);
                    }

                    TileEntityChest var18 = (TileEntityChest)var1.getBlockTileEntity(var8 + 3, var9, var10 + 1);

                    for (var13 = 0; var13 < 4; ++var13)
                    {
                        var20 = this.pickCheckLootItem2(var2);
                        var17 = this.pickCheckLootItem3(var2);

                        if (var20 != null)
                        {
                            var18.setInventorySlotContents(var2.nextInt(var18.getSizeInventory()), var20);
                            var18.setInventorySlotContents(var2.nextInt(var18.getSizeInventory()), var17);
                        }
                    }
                }
            }
        }
		*/
	}


	/**
	 * Renvoie le spawn total de tous une liste de batiment
	 * @param buildins
	 * @return
	 */
	private int totalRateSpawnByBuildingList (ArrayList<BuildingAndInfos> buildings) {
		int total = 0;
		for (BuildingAndInfos building : buildings) {
			total += building.spawnRate;
		}
		return total;
	}
	
	/**
	 * Charge le batiment de maniere aléatoire en fonction du ratio
	 * @param buildings
	 * @param totalRate
	 * @return
	 */
	private Building getBuildingInRate(ArrayList<BuildingAndInfos> buildings, Random random) {
		
		ArrayList<Building>buildingsForRate = new ArrayList<Building>();
		
		for (BuildingAndInfos buildingAndInfos : buildings) {
			for (int i = 0; i < buildingAndInfos.spawnRate; i++) {
				buildingsForRate.add(buildingAndInfos.building);
			}
		}
		
		return buildingsForRate.get(random.nextInt(this.totalRateSpawnByBuildingList (buildings)));
	}
	
	/**
	 * Retourne l'orientation retourner en fonction de la rotation
	 * @param rotate
	 * @param orientation
	 * @return
	 */
	private int rotateOrientation(int rotate, int orientation) {
		if (rotate == Building.ROTATED_90) {
			
			switch (orientation) { 
				case Unity.ORIENTATION_UP:
					return Unity.ORIENTATION_RIGTH;
				case Unity.ORIENTATION_RIGTH:
					return Unity.ORIENTATION_DOWN;
				case Unity.ORIENTATION_DOWN:
					return Unity.ORIENTATION_LEFT;
				case Unity.ORIENTATION_LEFT:
					return Unity.ORIENTATION_UP;
				default:
					return Unity.ORIENTATION_NONE;
			}
		}
		if (rotate == Building.ROTATED_180) {
			return this.rotateOrientation(Building.ROTATED_90, this.rotateOrientation(Building.ROTATED_90, orientation));
		}
		if (rotate == Building.ROTATED_240) {
			return this.rotateOrientation(Building.ROTATED_180, this.rotateOrientation(Building.ROTATED_90, orientation));
		}
		return orientation;
	}
	
	
	/**
	 * Insert le contenu du block
	 * @param world
	 * @param random
	 * @param i
	 * @param j
	 * @param k
	 * @param contents
	 */
	private void setContents(World world, Random random, int x, int y, int z, ArrayList<ArrayList<Content>> contents) {
		
		Block block  = Block.blocksList [world.getBlockId (x, y, z)];
		
		if (block instanceof BlockChest) {

			TileEntity te  = world.getBlockTileEntity (x, y, z);
			if (te instanceof TileEntityChest) {
				
				for (int i = 0; i < contents.size(); i++) {
					
					ArrayList<Content> groupItem = contents.get(i);
					
					// Recupère un item aléatoirement
					Content content = groupItem.get(random.nextInt (groupItem.size()));
					// Calcule le nombre aléatoire d'item
					int diff = content.max - content.min;
					int nombre = content.min + ((diff > 0) ? random.nextInt (diff) : 0);
					
					((TileEntityChest) te).setInventorySlotContents (i, new ItemStack(Item.itemsList[content.id], nombre));
				}
			}
			
		}
		
	}
	

	/**
	 * Affecte l'orientation
	 * @param i
	 * @param j
	 * @param k
	 * @param orientation
	 * @param rotation
	 */
	private void setOrientation(World world, int x, int y, int z, int orientation) {
		
		Block block  = Block.blocksList [world.getBlockId (x, y, z)];
		int metadata = world.getBlockMetadata (x, y, z);
		
		if (block instanceof BlockTorch) {

			if (orientation == Unity.ORIENTATION_NONE)  { metadata = (metadata & 0x8) + 0; } else 
			if (orientation == Unity.ORIENTATION_UP)    { metadata = (metadata & 0x8) + 4; } else 
			if (orientation == Unity.ORIENTATION_DOWN)  { metadata = (metadata & 0x8) + 3; } else 
			if (orientation == Unity.ORIENTATION_LEFT)  { metadata = (metadata & 0x8) + 2; } else 
			if (orientation == Unity.ORIENTATION_RIGTH) { metadata = (metadata & 0x8) + 1; } else 
			{
				ModCastleDefenders.log.severe("Bad orientation : "+x+","+y+","+z);
			}
			
			world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
			return;
		}
		
		if (block instanceof BlockDirectional) {

			if (orientation == Unity.ORIENTATION_NONE)  { metadata = (metadata & 0x8) + 0; } else 
			if (orientation == Unity.ORIENTATION_UP)    { metadata = (metadata & 0x8) + 0; } else 
			if (orientation == Unity.ORIENTATION_DOWN)  { metadata = (metadata & 0x8) + 2; } else 
			if (orientation == Unity.ORIENTATION_LEFT)  { metadata = (metadata & 0x8) + 3; } else 
			if (orientation == Unity.ORIENTATION_RIGTH) { metadata = (metadata & 0x8) + 1; } else 
			{
				ModCastleDefenders.log.severe("Bad orientation : "+x+","+y+","+z);
			}
			
			world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
			return;
		}
		
		if (
			block instanceof BlockLadder ||
			block instanceof BlockWall ||
			block instanceof BlockFurnace ||
			block instanceof BlockChest
		) {
			
			if (orientation == Unity.ORIENTATION_NONE)  { metadata = (metadata & 0x8) + 2; } else 
			if (orientation == Unity.ORIENTATION_UP)    { metadata = (metadata & 0x8) + 2; } else 
			if (orientation == Unity.ORIENTATION_DOWN)  { metadata = (metadata & 0x8) + 3; } else 
			if (orientation == Unity.ORIENTATION_LEFT)  { metadata = (metadata & 0x8) + 4; } else 
			if (orientation == Unity.ORIENTATION_RIGTH) { metadata = (metadata & 0x8) + 5; } else 
			{
				ModCastleDefenders.log.severe("Bad orientation : "+x+","+y+","+z);
			}
			
			world.setBlockMetadataWithNotify(x, y, z, metadata, 2);
			return;
		}
		
	}

}
