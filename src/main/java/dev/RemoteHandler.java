package dev;

import js.app.CmdLineArgs;
import js.json.JSList;
import js.json.JSMap;
import js.webtools.gen.RemoteEntityInfo;

public interface RemoteHandler {

  void create(CmdLineArgs args, String entityName, String imageName);

  JSMap listEntities();

  JSMap listEntitiesDetailed();

  void delete(String name);

  RemoteEntityInfo select(String name);

  String name();

  void createImage(String imageLabel);

  JSList getImagesList();
  
}
