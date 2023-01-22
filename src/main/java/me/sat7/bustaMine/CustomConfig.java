package me.sat7.bustaMine;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class CustomConfig {
    private File file; // java의 데이터타입
    private FileConfiguration customFile; // 버킷의 데이터 타입

    //Finds or generates the custom config file
    public void setup(String name){
        file = new File(BustaMine.plugin.getDataFolder(), name + ".yml");

        if (!file.exists()){
            try{
                file.createNewFile();
            }catch (IOException e){
                //System.out.println("CreateFileFail");
            }
        }
        customFile = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration get(){
        return customFile;
    }

    public void save(){
        try{
            customFile.save(file);
        }catch (IOException e){
            System.out.println("Couldn't save file");
        }
    }

    public void reload(){
        customFile = YamlConfiguration.loadConfiguration(file);
    }

    public void delete()
    {
        file.delete();
    }
}
