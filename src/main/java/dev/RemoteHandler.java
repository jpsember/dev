package dev;

import static js.base.Tools.*;

import java.io.File;

import js.app.CmdLineArgs;
import js.file.Files;
import js.json.JSMap;
import js.webtools.gen.RemoteEntityInfo;

public interface RemoteHandler {

  void create(CmdLineArgs args, String name);

  JSMap listEntities();

  JSMap listEntitiesDetailed();

  void delete(String name);

  RemoteEntityInfo select(String name);

  String name();
  
}
