import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

/** JDK-only reproducible model generator. Samples palette UVs; does not copy game textures. */
public class GenerateFoodModels {
    record Food(String id, String shape, String texture, int main, int accent) {}
    record Paint(String texture, double[] uv) {}
    private static ZipFile game;
    private static final List<Food> FOODS = new ArrayList<>();
    private final Map<String, Paint> paints = new LinkedHashMap<>();
    private final List<Object> elements = new ArrayList<>();

    static void food(String id, String shape, String texture, int main, int accent) {
        FOODS.add(new Food(id.contains(":") ? id : "minecraft:" + id, shape, texture, main, accent));
    }
    static {
        food("apple", "apple", "apple", 0xe33428, 0xff7355);
        food("golden_apple", "apple", "golden_apple", 0xe8b22c, 0xffed87);
        food("enchanted_golden_apple", "apple", "golden_apple", 0xf2be35, 0xfff29c);
        food("baked_potato", "potato", "baked_potato", 0xb37a36, 0xf4d881);
        food("potato", "potato", "potato", 0xc3a264, 0xe7cc8c);
        food("poisonous_potato", "potato", "poisonous_potato", 0xa9aa56, 0x637d35);
        food("beef", "steak", "beef", 0xb93b32, 0xf9bba4);
        food("cooked_beef", "steak", "cooked_beef", 0x71412b, 0xb77e4d);
        food("beetroot", "beetroot", "beetroot", 0x9f2443, 0xd9455d);
        food("beetroot_soup", "bowl", "beetroot_soup", 0x9f2443, 0xd9455d);
        food("bread", "loaf", "bread", 0xb47732, 0xe2b661);
        food("carrot", "carrot", "carrot", 0xef8d20, 0xffb837);
        food("golden_carrot", "carrot", "golden_carrot", 0xe5b325, 0xffed79);
        food("chicken", "chicken", "chicken", 0xf2b9a5, 0xd98b79);
        food("cooked_chicken", "chicken", "cooked_chicken", 0xc78247, 0xf1bd7b);
        food("chorus_fruit", "chorus", "chorus_fruit", 0x985b98, 0xc89fbe);
        food("cod", "fish", "cod", 0xaca08a, 0xd2c4a7);
        food("cooked_cod", "fish", "cooked_cod", 0xb6884e, 0xd7b17a);
        food("salmon", "fish", "salmon", 0xae6761, 0x536e62);
        food("cooked_salmon", "fish", "cooked_salmon", 0xb65b3f, 0xe19967);
        food("tropical_fish", "fish", "tropical_fish", 0xe97736, 0xf0e5c6);
        food("pufferfish", "puffer", "pufferfish", 0xd7ac43, 0xf1d77e);
        food("mutton", "chop", "mutton", 0xb33f3c, 0xe29a86);
        food("cooked_mutton", "chop", "cooked_mutton", 0x81502f, 0xb78351);
        food("porkchop", "chop", "porkchop", 0xe99a9c, 0xffc8b3);
        food("cooked_porkchop", "chop", "cooked_porkchop", 0xad7442, 0xd7a66b);
        food("rabbit", "rabbit", "rabbit", 0xca8584, 0xf1b2a0);
        food("cooked_rabbit", "rabbit", "cooked_rabbit", 0xa96f3e, 0xdbab6c);
        food("cookie", "cookie", "cookie", 0xb77b38, 0x533420);
        food("dried_kelp", "kelp", "dried_kelp", 0x425235, 0x253522);
        food("honey_bottle", "bottle", "honey_bottle", 0xe1a42c, 0xffd955);
        food("melon_slice", "melon", "melon_slice", 0xe5473c, 0x4b8534);
        food("mushroom_stew", "bowl", "mushroom_stew", 0x96734b, 0xc9a06d);
        food("rabbit_stew", "bowl", "rabbit_stew", 0xb17d46, 0xe4b674);
        food("suspicious_stew", "bowl", "suspicious_stew", 0x7c693f, 0xb2925b);
        food("pumpkin_pie", "pie", "pumpkin_pie", 0xc97c28, 0xefb65a);
        food("rotten_flesh", "rotten", "rotten_flesh", 0x98613c, 0x647035);
        food("spider_eye", "eye", "spider_eye", 0xb33c58, 0x611f34);
        food("sweet_berries", "berries", "sweet_berries", 0xb82b3b, 0xf35555);
        food("glow_berries", "berries", "glow_berries", 0xe4a232, 0xffd55b);
        food("speedapple:speed_apple", "speedapple", "apple", 0xe33428, 0xff7355);
        food("speedapple:night_vision_carrot", "nightcarrot", "golden_carrot", 0xe5b325, 0xffed79);
        food("speedapple:jump_gummy", "gummy", "slime_ball", 0x75b64b, 0xa7d47e);
    }

    void paint(String name, String texture, int rgb) throws IOException {
        String resource = texture.contains(":") ? texture : "minecraft:item/" + texture;
        String[] parts = resource.split(":", 2);
        var entry = game.getEntry("assets/" + parts[0] + "/textures/" + parts[1] + ".png");
        if (entry == null) throw new IOException("Texture missing: " + resource);
        BufferedImage image;
        try (InputStream input = game.getInputStream(entry)) { image = ImageIO.read(input); }
        long best = Long.MAX_VALUE;
        int px = -1, py = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int c = image.getRGB(x, y);
            if ((c >>> 24) < 200) continue;
            int dr = ((c >> 16) & 255) - ((rgb >> 16) & 255);
            int dg = ((c >> 8) & 255) - ((rgb >> 8) & 255);
            int db = (c & 255) - (rgb & 255);
            long distance = 2L * dr * dr + 3L * dg * dg + db * db;
            if (distance < best) { best = distance; px = x; py = y; }
        }
        if (px < 0) throw new IOException("No opaque palette pixels: " + resource);
        paints.put(name, new Paint(resource, new double[] {
            (px + 0.15) * 16 / image.getWidth(), (py + 0.15) * 16 / image.getHeight(),
            (px + 0.85) * 16 / image.getWidth(), (py + 0.85) * 16 / image.getHeight() }));
    }

    void box(double x1, double y1, double z1, double x2, double y2, double z2, String color) {
        box(x1, y1, z1, x2, y2, z2, color, color);
    }
    void box(double x1, double y1, double z1, double x2, double y2, double z2, String color, String top) {
        if (!(x2 > x1 && y2 > y1 && z2 > z1)) throw new IllegalArgumentException("Empty geometry");
        Map<String, Object> faces = new LinkedHashMap<>();
        for (String face : List.of("north", "south", "east", "west", "up", "down")) {
            String key = face.equals("up") ? top : color;
            Paint paint = Objects.requireNonNull(paints.get(key), key);
            faces.put(face, Map.of("uv", paint.uv, "texture", "#" + key));
        }
        elements.add(Map.of("from", new double[]{x1, y1, z1}, "to", new double[]{x2, y2, z2}, "faces", faces));
    }
    void round(double x, double y, double z, double width, double height, double depth, String color) {
        box(x-width/2+1, y, z-depth/2+1, x+width/2-1, y+height, z+depth/2-1, color, "light");
        box(x-width/2, y+height*0.2, z-depth/2+1, x+width/2, y+height*0.8, z+depth/2-1, color, "light");
        box(x-width/2+1, y+height*0.2, z-depth/2, x+width/2-1, y+height*0.8, z+depth/2, color, "light");
    }
    void ring(double lo, double hi, double y, double thickness, String color) {
        box(lo,y,lo,hi,y+thickness,lo+1,color);
        box(lo,y,hi-1,hi,y+thickness,hi,color);
        box(lo,y,lo+1,lo+1,y+thickness,hi-1,color);
        box(hi-1,y,lo+1,hi,y+thickness,hi-1,color);
    }
    void apple(boolean speed) {
        round(8,0,8,10,9,9,"main");
        box(4,8.3,5,7,9.3,11,"light"); box(9,8.3,5,12,9.3,11,"main");
        box(7.5,9,7.5,8.5,11.5,8.5,"wood"); box(8.5,10,7.5,11.5,10.5,9,"leaf");
        box(3,4.5,5,3.3,6.5,6,"light");
        if (speed) { box(6.5,5.5,3.4,8,7.5,3.7,"gold"); box(7.5,3.2,3.4,9,5.8,3.7,"gold"); }
    }
    void carrot(boolean night) {
        box(6,0,4,10,3.5,8,"main","light");
        box(6.5,0.25,8,9.5,3,11,"main","light");
        box(7,0.6,11,9,2.6,13,"main"); box(7.5,1,13,8.5,2,15,"main");
        box(7.3,1.2,1,8.7,4.5,4.2,"leaf"); box(5.5,2,1.5,7.4,3.3,4.2,"leaf");
        box(8.6,2,1,10.5,3.3,4.2,"leaf"); box(5.9,1,5.5,6.15,1.5,7,"dark");
        if (night) { box(6.8,3.5,5.5,7.7,3.8,6.6,"black"); box(8.3,3.5,5.5,9.2,3.8,6.6,"black"); }
    }
    void fish(Food food) {
        round(8,0.5,8,5,5,9,"main");
        box(6.2,1.3,2,9.8,4.5,4.7,"main","light");
        box(6.4,0,12,9.6,6,13,"accent"); box(5.5,0,13,10.5,6,15,"accent");
        box(7.5,4.5,6,8.5,7,10,"accent");
        box(5,1.5,6.5,6.5,2.2,9,"accent"); box(9.5,1.5,6.5,11,2.2,9,"accent");
        box(6,3,2.9,6.2,4,3.9,"white"); box(9.8,3,2.9,10,4,3.9,"white");
        box(5.95,3.2,3,6.05,3.8,3.6,"black"); box(9.95,3.2,3,10.05,3.8,3.6,"black");
        if (food.id.endsWith("tropical_fish")) { box(5.45,1.7,5.8,10.55,4.4,6.7,"white"); box(5.45,1.7,9.5,10.55,4.4,10.2,"white"); }
        else { box(7,5,6,9,5.4,11,"accent"); }
    }
    void meat(boolean chop) {
        round(8,0,8,chop?9:11,3.5,chop?10:8,"accent");
        round(8,0.5,8,chop?7:9,3.4,chop?8:6,"main");
        box(5.5,3.7,6,6.4,4.1,9,"accent"); box(8.7,3.7,5.7,9.6,4.1,8.7,"accent");
        if(chop) { box(7,3.5,9.7,9,4.2,11.5,"white"); box(7.6,3.5,11.5,8.4,4.2,12.8,"white"); }
    }
    void chicken(boolean rabbit) {
        round(8,0,7.5,8,6,9,"main");
        box(3,1,5,5,3.5,9,"accent"); box(11,1,5,13,3.5,9,"accent");
        box(4.5,0,10,7,2.5,rabbit?14.5:13,"main"); box(9,0,10,11.5,2.5,rabbit?14.5:13,"main");
        box(5,0.6,rabbit?14:12.5,6.5,1.7,15,"white"); box(9.5,0.6,rabbit?14:12.5,11,1.7,15,"white");
        box(7,5.4,5,9,6.3,9,"light");
    }
    void bowl(Food food) {
        box(5,0,5,11,1.2,11,"wood"); ring(4,12,1,1,"wood"); ring(3,13,2,1.8,"wood");
        ring(2.7,13.3,3.7,0.6,"woodlight"); box(4,2,4,12,3.55,12,"main","light");
        box(5,3.55,5,6.5,4,6.5,"accent"); box(8,3.55,8,10,4,9.5,"accent");
        box(6,3.55,9.5,7.5,3.9,11,"leaf"); box(10,3.55,5.5,11,4,7,"leaf");
        if(food.id.contains("rabbit")) { box(7,3.55,6,8,4.2,7.5,"carrot"); box(5,3.55,8,6.5,4.1,9,"white"); }
        if(food.id.contains("suspicious")) { box(9,3.6,10,10,4.2,11,"purple"); }
    }
    void build(Food f) {
        switch(f.shape) {
            case "apple", "speedapple" -> apple(f.shape.equals("speedapple"));
            case "carrot", "nightcarrot" -> carrot(f.shape.equals("nightcarrot"));
            case "fish" -> fish(f);
            case "steak", "chop" -> meat(f.shape.equals("chop"));
            case "chicken", "rabbit" -> chicken(f.shape.equals("rabbit"));
            case "bowl" -> bowl(f);
            case "potato" -> {
                round(8,0,8,9,5.5,7,"main");
                box(6,4.7,6,10,5.8,9,"accent");
                box(3.45,2,7,3.7,2.8,8,"dark"); box(10.5,3.8,10.5,11.2,4.2,11.2,"dark");
                if(f.id.contains("baked")) { box(6.5,5,5.5,9.5,6,10.5,"light"); box(7.3,5.5,6,8.7,6.2,10,"white"); }
            }
            case "beetroot" -> {
                round(8,0.5,8,7,5,7,"main"); box(7.5,0,7.5,8.5,1,8.5,"dark");
                box(7,5,7,9,8.5,9,"leaf"); box(4.5,6,6,7.5,7.5,9,"leaf"); box(8.5,7,7,11,8.3,10,"leaf");
            }
            case "loaf" -> {
                box(2,0,5,14,3.5,11,"main","light"); box(3,1,4,13,5,12,"main","light");
                box(4,4,5,12,7,11,"main","light"); box(5,6.5,6,11,7.5,10,"light");
                for(int x=5;x<=10;x+=2)box(x,7.48,6.1,x+0.75,7.65,9.9,"accent");
            }
            case "chorus" -> {
                round(8,0,8,9,7.5,9,"main"); box(6,7,6,10,9,10,"accent");
                box(4,2,3.3,6,4.5,3.6,"light"); box(9,5,3.3,11,6.5,3.6,"dark");
            }
            case "puffer" -> {
                round(8,0,8,9,7,9,"main");
                box(3,3,6,4,5,8,"accent"); box(12,3,6,13,5,8,"accent");
                box(6,6,7,7,8.5,8,"accent"); box(9,6,9,10,8,10,"accent");
                box(5,2,3.5,6.5,4.5,3.8,"white"); box(9.5,2,3.5,11,4.5,3.8,"white");
                box(5.4,2.5,3.35,6.1,3.8,3.6,"black"); box(9.9,2.5,3.35,10.6,3.8,3.6,"black");
                box(7.3,0.8,3.4,8.7,1.8,3.7,"dark"); box(6.5,2,12,9.5,5,14,"accent");
            }
            case "cookie" -> {
                box(3,0,4,13,2,12,"main","light"); box(4,0,3,12,2,13,"main","light");
                for(double[] p:List.of(new double[]{5,5},new double[]{9,4.5},new double[]{7,8},new double[]{10,10},new double[]{4.5,10}))box(p[0],2,p[1],p[0]+1.2,2.3,p[1]+1.2,"accent");
            }
            case "kelp" -> {
                box(3,0,3,13,0.8,7,"main"); box(3,0.5,7,13,1.5,10,"accent"); box(3,0,10,13,0.8,13,"main");
                for(int x=4;x<13;x+=3)box(x,0.8,3,x+0.5,1,7,"dark");
            }
            case "bottle" -> {
                box(5,0,5,11,1,11,"glass"); box(5.3,1,5.3,10.7,7.5,10.7,"main","light");
                for(int x:List.of(5,10))for(int z:List.of(5,10))box(x,1,z,x+1,8,z+1,"glass");
                box(5.5,7.5,5.5,10.5,8.5,10.5,"glass"); box(6.3,8.5,6.3,9.7,11,9.7,"glass");
                box(6,10.5,6,10,12.5,10,"wood"); box(5,4,4.9,7,6.5,5.1,"white");
            }
            case "melon" -> {
                box(6,0,6,10,1.5,10,"accent"); box(4,1,6,12,3,10,"accent"); box(2,3,6,14,7,10,"accent");
                box(5,2,5.9,11,4,10.1,"main","light"); box(3,4,5.9,13,7,10.1,"main","light");
                for(double[] p:List.of(new double[]{5,5},new double[]{8,3},new double[]{10,5.5})) { box(p[0],p[1],5.7,p[0]+0.6,p[1]+1,5.95,"black"); box(p[0],p[1],10.05,p[0]+0.6,p[1]+1,10.3,"black"); }
            }
            case "pie" -> {
                box(3,0,4,13,2,12,"woodlight"); box(4,0,3,12,2,13,"woodlight");
                ring(3,13,1.6,1.5,"woodlight"); box(4,1.8,4,12,3,12,"main","light");
                for(int a=4;a<13;a+=2){box(a,3,2.8,a+1,3.5,4,"woodlight");box(a,3,12,a+1,3.5,13.2,"woodlight");}
            }
            case "rotten" -> {
                round(8,0,8,9,3.8,8,"main"); box(3,0,5,6,2,8,"accent");
                box(8,3.5,7,11,4.2,9,"accent"); box(4.5,2.8,9,6,3.8,11,"accent");
            }
            case "eye" -> {
                round(8,0,8,7,6,7,"main"); box(5,1,4.3,11,5,4.7,"accent");
                box(6.5,1.5,4.15,9.5,4.5,4.45,"black"); box(6.8,3.3,4.05,7.7,4.2,4.25,"white");
                box(7.5,1,11,8.5,2.5,14,"dark");
            }
            case "berries" -> {
                for(double[] p:List.of(new double[]{5,0,6},new double[]{8,0,5},new double[]{7,0,9},new double[]{10,0,8},new double[]{7,2.5,7}))box(p[0]-1.4,p[1],p[2]-1.4,p[0]+1.4,p[1]+2.8,p[2]+1.4,"main","light");
                box(7,4.5,6,8.5,6.5,8,"leaf"); box(4.5,5.5,7,7.5,6,9,"leaf"); box(8,5.5,6,11,6,8,"leaf");
            }
            case "gummy" -> {
                round(8,0,8,7,8,6,"main"); box(5,7,6,7,10,8,"main","light"); box(9,7,6,11,10,8,"main","light");
                box(4,2,6,5.5,5,8,"accent"); box(10.5,2,6,12,5,8,"accent");
                box(5,0,5,7.5,1.5,7,"main"); box(8.5,0,5,11,1.5,7,"main");
                box(6,5.2,4.9,7.2,6.5,5.2,"white"); box(8.8,5.2,4.9,10,6.5,5.2,"white");
                box(6.4,5.4,4.8,6.9,6.1,5,"black"); box(9.1,5.4,4.8,9.6,6.1,5,"black");
            }
            default -> throw new IllegalArgumentException("Unknown food shape: "+f.shape);
        }
    }

    Map<String,Object> model(Food f) throws IOException {
        paint("main",f.texture,f.main); paint("light",f.texture,bright(f.main,1.3));
        paint("dark",f.texture,bright(f.main,0.55)); paint("accent",f.texture,f.accent);
        paint("leaf","wheat_seeds",0x517f2b); paint("wood","bowl",0x795129);
        paint("woodlight","bread",0xd5a65b); paint("white","bone_meal",0xeeeeee);
        paint("black","ink_sac",0x151515); paint("gold","golden_apple",0xffdc57);
        paint("glass","honey_bottle",0xdad1a6); paint("carrot","carrot",0xf79c29);
        paint("purple","chorus_fruit",0x8f558f);
        build(f);
        Map<String,Object> textures=new LinkedHashMap<>(); paints.forEach((n,p)->textures.put(n,p.texture)); textures.put("particle",paints.get("main").texture);
        return Map.of("parent","minecraft:block/block","ambientocclusion",false,"gui_light","front",
            "textures",textures,"elements",elements,"display",Map.of(
                "gui",display(new double[]{25,225,0},new double[]{0,2,0},0.95),
                "ground",display(new double[]{0,0,0},new double[]{0,0,0},0.85),
                "fixed",display(new double[]{0,0,0},new double[]{0,1,0},0.9),
                "firstperson_righthand",display(new double[]{0,-30,0},new double[]{1.5,1,1},0.7),
                "firstperson_lefthand",display(new double[]{0,30,0},new double[]{1.5,1,1},0.7),
                "thirdperson_righthand",display(new double[]{75,45,0},new double[]{0,2,1},0.55),
                "thirdperson_lefthand",display(new double[]{75,-45,0},new double[]{0,2,1},0.55)));
    }
    static Map<String,Object> display(double[] rotation,double[] translation,double scale) {
        return Map.of("rotation",rotation,"translation",translation,"scale",new double[]{scale,scale,scale});
    }
    static int bright(int c,double scale) { return ((int)Math.min(255,((c>>16)&255)*scale)<<16)|((int)Math.min(255,((c>>8)&255)*scale)<<8)|(int)Math.min(255,(c&255)*scale); }
    static String json(Object value) {
        if(value instanceof String s)return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";
        if(value instanceof double[] a)return json(Arrays.stream(a).boxed().toList());
        if(value instanceof List<?> l)return "["+String.join(",",l.stream().map(GenerateFoodModels::json).toList())+"]";
        if(value instanceof Map<?,?> m){List<String> entries=new ArrayList<>();m.entrySet().stream().sorted(Comparator.comparing(e->e.getKey().toString())).forEach(e->entries.add(json(e.getKey().toString())+":"+json(e.getValue())));return "{"+String.join(",",entries)+"}";}
        return String.valueOf(value);
    }
    static void write(Path p,Object value)throws IOException{Files.createDirectories(p.getParent());Files.writeString(p,json(value)+"\n",StandardCharsets.UTF_8);}
    public static void main(String[] args)throws Exception {
        if(args.length!=2)throw new IllegalArgumentException("Usage: java tools/GenerateFoodModels.java <minecraft-client.jar> <resources-directory>");
        Path out=Path.of(args[1]); List<Object> catalog=new ArrayList<>();
        try(ZipFile archive=new ZipFile(args[0])) {game=archive;
            for(Food food:FOODS){String[] id=food.id.split(":");String model="tabletopfoods:item/"+id[0]+"/"+id[1];
                write(out.resolve("assets/tabletopfoods/models/item/"+id[0]+"/"+id[1]+".json"),new GenerateFoodModels().model(food));
                write(out.resolve("assets/"+id[0]+"/items/"+id[1]+".json"),Map.of("model",Map.of("type","minecraft:model","model",model)));
                catalog.add(Map.of("id",food.id,"shape",food.shape,"texture","minecraft:item/"+food.texture));
            }
        }
        write(out.resolve("tabletopfoods-food-models.json"),catalog);
        System.out.println("Generated "+FOODS.size()+" sculpted food models with existing texture references.");
    }
}
