package com.promeia.music;

import android.content.*;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.documentfile.provider.DocumentFile;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

final class Store {
    static final String PREF="promeia";
    static Store instance;
    final Context c; final SharedPreferences p;
    final java.util.concurrent.CopyOnWriteArrayList<Song> songs=new java.util.concurrent.CopyOnWriteArrayList<>(); final java.util.concurrent.CopyOnWriteArrayList<Playlist> lists=new java.util.concurrent.CopyOnWriteArrayList<>();
    static synchronized Store get(Context c){if(instance==null)instance=new Store(c.getApplicationContext());return instance;}
    Store(Context c){this.c=c;p=c.getSharedPreferences(PREF,0);load(p.getString("library","{}"));}
    static class Song {
        String id="", uri="", title="", artist="", album="", cover="", hash=""; long duration; boolean favorite;
        Song(){} Song(JSONObject j){id=j.optString("id");uri=j.optString("uri");title=j.optString("title");artist=j.optString("artist");album=j.optString("album");cover=j.optString("cover");hash=j.optString("hash");duration=j.optLong("duration");favorite=j.optBoolean("favorite");}
        JSONObject json() throws JSONException{return new JSONObject().put("id",id).put("uri",uri).put("title",title).put("artist",artist).put("album",album).put("cover",cover).put("hash",hash).put("duration",duration).put("favorite",favorite);}
    }
    static class Playlist {
        String id=UUID.randomUUID().toString(), name="", cover=""; final ArrayList<String> ids=new ArrayList<>();
        Playlist(){} Playlist(JSONObject j){id=j.optString("id",id);name=j.optString("name");cover=j.optString("cover");JSONArray a=j.optJSONArray("songs");if(a!=null)for(int i=0;i<a.length();i++)ids.add(a.optString(i));}
        JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("name",name).put("cover",cover).put("songs",new JSONArray(ids));}
    }
    synchronized void load(String json){try{JSONObject j=new JSONObject(json);ArrayList<Song> ss=new ArrayList<>();ArrayList<Playlist> pp=new ArrayList<>();JSONArray a=j.optJSONArray("songs"),b=j.optJSONArray("playlists");if(a!=null)for(int i=0;i<a.length();i++)ss.add(new Song(a.getJSONObject(i)));if(b!=null)for(int i=0;i<b.length();i++)pp.add(new Playlist(b.getJSONObject(i)));songs.clear();songs.addAll(ss);lists.clear();lists.addAll(pp);}catch(Exception e){throw new IllegalArgumentException("Biblioteca inválida",e);}}
    synchronized String snapshot(){try{JSONArray a=new JSONArray(),b=new JSONArray();for(Song s:songs)a.put(s.json());for(Playlist l:lists)b.put(l.json());return new JSONObject().put("songs",a).put("playlists",b).toString(2);}catch(Exception e){throw new IllegalStateException(e);}}
    synchronized void save(){String json=snapshot();p.edit().putString("library",json).apply();try{DocumentFile r=actualRoot();if(r!=null){DocumentFile f=r.findFile("biblioteca.json");if(f==null)f=r.createFile("application/json","biblioteca.json");if(f!=null)try(OutputStream out=c.getContentResolver().openOutputStream(f.getUri(),"wt")){out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}}}catch(Exception ignored){}}
    synchronized Song find(String id){for(Song s:songs)if(s.id.equals(id))return s;return null;}
    DocumentFile root(){String r=p.getString("root","");return r.isEmpty()?null:DocumentFile.fromTreeUri(c,Uri.parse(r));}
    synchronized void setRoot(Uri u)throws Exception{DocumentFile base=DocumentFile.fromTreeUri(c,u);if(base==null||!base.canWrite())throw new IOException("Carpeta sin permiso de escritura");DocumentFile r=base;if(!"Promeia Music".equals(base.getName())){r=base.findFile("Promeia Music");if(r==null)r=base.createDirectory("Promeia Music");}if(r==null||!r.isDirectory())throw new IOException("No se pudo crear Promeia Music");if(r.findFile("Music")==null&&r.createDirectory("Music")==null)throw new IOException("No se pudo crear Music");if(r.findFile("Covers")==null&&r.createDirectory("Covers")==null)throw new IOException("No se pudo crear Covers");
        // Store the chosen tree URI, and resolve its child via DocumentFile on every access.
        String old=p.getString("tree","");if(!songs.isEmpty()&&!old.equals(u.toString()))throw new IOException("Exporte o elimine la biblioteca antes de cambiar su carpeta");
        DocumentFile db=r.findFile("biblioteca.json");if(songs.isEmpty()&&db!=null){try(InputStream in=c.getContentResolver().openInputStream(db.getUri())){load(new String(readAll(in,20*1024*1024),java.nio.charset.StandardCharsets.UTF_8));}}
        p.edit().putString("tree",u.toString()).putString("root",u.toString()).putBoolean("rootChild",!"Promeia Music".equals(base.getName())).apply();save();}
    DocumentFile actualRoot(){DocumentFile r=root();return r==null?null:(p.getBoolean("rootChild",false)?r.findFile("Promeia Music"):r);}
    DocumentFile folder(String name)throws IOException{DocumentFile r=actualRoot();if(r==null)throw new IOException("Seleccione la carpeta de almacenamiento primero");DocumentFile f=r.findFile(name);if(f==null)f=r.createDirectory(name);if(f==null||!f.isDirectory())throw new IOException("Carpeta no disponible");return f;}
    String displayName(Uri u){try(Cursor cur=c.getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cur!=null&&cur.moveToFirst())return cur.getString(0);}catch(Exception ignored){}return "Audio";}
    static String clean(String x){return x.replaceAll("[\\\\/:*?\"<>|]","_").trim();}
    static byte[] readAll(InputStream in,int max)throws IOException{if(in==null)throw new IOException("Archivo no disponible");ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(o.size()+n>max)throw new IOException("Archivo demasiado grande");o.write(buf,0,n);}return o.toByteArray();}
    static String hex(byte[] a){StringBuilder s=new StringBuilder();for(byte b:a)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    synchronized Song importAudio(Uri u)throws Exception{String name=displayName(u),type=c.getContentResolver().getType(u);if(type==null||!type.startsWith("audio/")){String ext=name.toLowerCase(Locale.ROOT);if(!(ext.matches(".*\\.(mp3|m4a|aac|flac|ogg|opus|wav|wma|amr|mp4)$")))throw new IOException("Formato de audio no reconocido");type=ext.endsWith(".flac")?"audio/flac":ext.endsWith(".m4a")||ext.endsWith(".mp4")?"audio/mp4":ext.endsWith(".wav")?"audio/wav":ext.endsWith(".ogg")||ext.endsWith(".opus")?"audio/ogg":"audio/mpeg";}
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(u)){if(in==null)throw new IOException("Archivo no disponible");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}String hash=hex(digest.digest());for(Song existing:songs)if(existing.hash.equals(hash))return null;
        Song s=new Song();s.id=UUID.randomUUID().toString();s.hash=hash;s.title=name.replaceFirst("\\.[^.]+$","");s.artist="Artista desconocido";s.album="Sin álbum";byte[] art=null;
        MediaMetadataRetriever mr=new MediaMetadataRetriever();try{mr.setDataSource(c,u);String v=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);if(v!=null&&!v.trim().isEmpty())s.title=v;v=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);if(v!=null&&!v.trim().isEmpty())s.artist=v;v=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);if(v!=null&&!v.trim().isEmpty())s.album=v;v=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(v!=null)s.duration=Long.parseLong(v);art=mr.getEmbeddedPicture();}finally{try{mr.release();}catch(Exception ignored){}}
        if(s.duration<=0)throw new IOException("No se pudo reconocer audio reproducible");DocumentFile f=folder("Music").createFile(type,clean(name));if(f==null)throw new IOException("No se pudo crear el archivo");try(InputStream in=c.getContentResolver().openInputStream(u);OutputStream out=c.getContentResolver().openOutputStream(f.getUri())){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}catch(Exception e){f.delete();throw e;}s.uri=f.getUri().toString();if(art!=null)try{s.cover=writeCover(s.id,art);}catch(Exception ignored){}songs.add(s);save();return s;}
    String writeCover(String id,byte[] bytes)throws IOException{BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);if(opt.outWidth<=0||opt.outHeight<=0)throw new IOException("Imagen no válida");opt.inJustDecodeBounds=false;opt.inSampleSize=1;while(Math.max(opt.outWidth,opt.outHeight)/opt.inSampleSize>1400)opt.inSampleSize*=2;Bitmap bm=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opt);if(bm==null)throw new IOException("Imagen no válida");DocumentFile dir=folder("Covers"),old=dir.findFile(id+".jpg");DocumentFile f=old==null?dir.createFile("image/jpeg",id+".jpg"):old;if(f==null)throw new IOException("No se pudo crear carátula");try(OutputStream o=c.getContentResolver().openOutputStream(f.getUri(),"wt")){if(!bm.compress(Bitmap.CompressFormat.JPEG,90,o))throw new IOException("Error al guardar imagen");}finally{bm.recycle();}return f.getUri().toString();}
    synchronized void remove(Song s)throws IOException{DocumentFile f=DocumentFile.fromSingleUri(c,Uri.parse(s.uri));if(f!=null&&f.exists()&&!f.delete())throw new IOException("No se pudo borrar el archivo");if(!s.cover.isEmpty()){DocumentFile art=DocumentFile.fromSingleUri(c,Uri.parse(s.cover));if(art!=null)art.delete();}songs.remove(s);for(Playlist l:lists)l.ids.remove(s.id);save();}
}
