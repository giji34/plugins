package com.github.giji34.t;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class BlockPropertyHelper {
    public static HashMap<String, String> Properties(String blockData) {
        int begin = blockData.indexOf("[");
        int end = blockData.indexOf("]");
        HashMap<String, String> result = new HashMap<>();
        if (begin < 0 || end < 0) {
            return result;
        }
        String propsString = blockData.substring(begin + 1, end);
        String[] props = propsString.split(",");
        for (String prop : props) {
            String[] kv = prop.split("=");
            if (kv.length < 2) {
                continue;
            }
            result.put(kv[0], kv[1]);
        }
        return result;
    }

    public static String[] AvailableProperties(Material material, Server server) {
        String defaultBlockData = server.createBlockData(material).getAsString(false);
        HashMap<String, String> props = Properties(defaultBlockData);
        return props.keySet().toArray(new String[]{});
    }


    public static String MergeBlockData(String existing, String next, Server server) {
        HashMap<String, String> existingProps = Properties(existing);
        HashMap<String, String> nextProps = Properties(next);
        BlockData blockData = server.createBlockData(next);
        Material nextMaterial = blockData.getMaterial();
        String[] availableProps = AvailableProperties(nextMaterial, server);
        HashMap<String, String> resultProps = new HashMap<>();
        for (String key : existingProps.keySet()) {
            if (Arrays.asList(availableProps).contains(key)) {
                resultProps.put(key, existingProps.get(key));
            }
        }
        for (String key : nextProps.keySet()) {
            resultProps.put(key, nextProps.get(key));
        }
        String result = nextMaterial.getKey().toString();
        if (resultProps.size() > 0) {
            StringBuilder props = new StringBuilder();
            for (String key : resultProps.keySet()) {
                if (props.length() > 0) {
                    props.append(",");
                }
                props.append(key + "=" + resultProps.get(key));
            }
            result += "[" + props + "]";
        }
        return result;
    }

    public static boolean RotatePropertyValue(BlockData blockData, String name) {
        if (name.equals("facing") && blockData instanceof Directional) {
            Directional directional = (Directional)blockData;
            BlockFace face = directional.getFacing();
            BlockFace next = Rotate(directional.getFaces(), face);
            directional.setFacing(next);
        } else if (name.equals("axis") && blockData instanceof Orientable) {
            Orientable orientable = (Orientable) blockData;
            Axis axis = orientable.getAxis();
            Axis next = Rotate(orientable.getAxes(), axis);
            orientable.setAxis(next);
        } else if (name.equals("age") && blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            int age = ageable.getAge();
            int nextAge = (age + 1) % ageable.getMaximumAge();
            ageable.setAge(nextAge);
        } else if (name.equals("half") && blockData instanceof Bisected) {
            Bisected bisected = (Bisected) blockData;
            Bisected.Half current = bisected.getHalf();
            Bisected.Half next = Rotate(Bisected.Half.values(), current);
            bisected.setHalf(next);
        } else if (name.equals("shape") && blockData instanceof Stairs) {
            Stairs stairs = (Stairs) blockData;
            Stairs.Shape current = stairs.getShape();
            Stairs.Shape next = Rotate(Stairs.Shape.values(), current);
            stairs.setShape(next);
        } else if (name.equals("type") && blockData instanceof Slab) {
            Slab slab = (Slab) blockData;
            Slab.Type current = slab.getType();
            Slab.Type next = Rotate(Slab.Type.values(), current);
            slab.setType(next);
        } else if (name.equals("type") && blockData instanceof Chest) {
            Chest chest = (Chest) blockData;
            Chest.Type current = chest.getType();
            Chest.Type next = Rotate(Chest.Type.values(), current);
            chest.setType(next);
        } else if (name.equals("moisture") && blockData instanceof Farmland) {
            Farmland farmland = (Farmland) blockData;
            int current = farmland.getMoisture();
            int next = (current + 1) % (farmland.getMaximumMoisture() + 1);
            farmland.setMoisture(next);
        } else if (name.equals("shape") && blockData instanceof Rail) {
            Rail rail = (Rail) blockData;
            Rail.Shape current = rail.getShape();
            Rail.Shape next = Rotate(rail.getShapes(), current);
            rail.setShape(next);
        } else if (name.equals("face") && blockData instanceof FaceAttachable) {
            FaceAttachable faceAttachable = (FaceAttachable) blockData;
            FaceAttachable.AttachedFace current = faceAttachable.getAttachedFace();
            FaceAttachable.AttachedFace next = Rotate(FaceAttachable.AttachedFace.values(), current);
            faceAttachable.setAttachedFace(next);
        } else if (blockData instanceof Snow) {
            Snow snow = (Snow) blockData;
            int min = snow.getMinimumLayers();
            int max = snow.getMaximumLayers() + 1;
            int current = snow.getLayers() - min;
            int next = (current + 1) % (max - min) + min;
            snow.setLayers(next);
        } else {
            return false;
        }
        return true;
    }

    private static <T> T Rotate(T[] available, T current) {
        int index = -1;
        for (int i = 0; i < available.length; i++) {
            if (current.equals(available[i])) {
                index = i;
                break;
            }
        }
        int nextIndex = (index + 1) % available.length;
        return (T)available[nextIndex];
    }

    private static <T> T Rotate(Set<T> a, T current) {
        int index = -1;
        Object[] available = a.toArray();
        for (int i = 0; i < available.length; i++) {
            if (current.equals(available[i])) {
                index = i;
                break;
            }
        }
        int nextIndex = (index + 1) % available.length;
        return (T)available[nextIndex];
    }
}