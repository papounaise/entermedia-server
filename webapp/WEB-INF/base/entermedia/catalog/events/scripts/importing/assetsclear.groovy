package importing

import org.entermediadb.asset.Asset
import org.entermediadb.asset.MediaArchive
import org.entermediadb.asset.scanner.HotFolderManager
import org.entermediadb.asset.search.AssetSearcher
import org.openedit.Data
import org.openedit.hittracker.HitTracker
import org.openedit.hittracker.SearchQuery
import org.openedit.page.manage.PageManager

public void init()
{
	MediaArchive archive = context.getPageValue("mediaarchive");//Search for all files looking for videos
	
	
	//Make sure all the hot folders are connected to some assets
	HotFolderManager manager = (HotFolderManager)archive.getModuleManager().getBean("hotFolderManager");
	PageManager pageManager = archive.getPageManager();
	
	Collection hits = manager.loadFolders( archive.getCatalogId() );
	for(Iterator iterator = hits.iterator(); iterator.hasNext();)
	{
		Data folder = (Data)iterator.next();
		String base = "/WEB-INF/data/" + archive.getCatalogId() + "/originals";
		String name = folder.get("subfolder");
		String path = base + "/" + name ;
		List paths = pageManager.getChildrenPaths(path);
		if( paths.size() == 0)
		{
			log.info("Found hot folder with no files, canceled delete request " + path);
			return;
		}
	}
	
	List children = pageManager.getChildrenPaths("/WEB-INF/data/" + archive.getCatalogId() + "/originals");
	if(children.size() == 0 )
	{
		log.info("No originals found. Skipping clear");
		return;	
	}

	AssetSearcher searcher = archive.getAssetSearcher();
	SearchQuery q = searcher.createSearchQuery();
	HitTracker assets = null;
	String sourcepath = context.getRequestParameter("sourcepath");
	if(sourcepath == null)
	{
		q = searcher.createSearchQuery().append("category", "index");
		q.addNot("editstatus","7");
	}
	else
	{
		q.addStartsWith("sourcepath", sourcepath);
	}
	q.addSortBy("sourcepath");
	assets = searcher.search(q);
	assets.enableBulkOperations();
	int removed = 0;
	List tosave = new ArrayList();
	int existed = 0;	
	for(Object obj: assets)
	{
		Data hit = (Data)obj;
	
		String assetsource = hit.get("archivesourcepath");
		if( assetsource == null)
		{
			assetsource = hit.getSourcePath();
		}
		String pathToOriginal = "/WEB-INF/data" + archive.getCatalogHome() + "/originals/" + assetsource;
		
		if(!pageManager.getRepository().doesExist(pathToOriginal) )
		{
			Asset asset = archive.getAssetBySourcePath(assetsource);
			if( asset == null)
			{
				log.info("invalid asset " + path);
				continue;
			}

			if(asset.isFolder() && asset.getPrimaryFile() != null)
			{
				pathToOriginal = pathToOriginal + "/" + asset.getPrimaryFile();
				if( pageManager.getRepository().doesExist(pathToOriginal) )
				{
					existed++;
					continue; //never mind, it is here
				}
			}
			removed++;
			//archive.removeGeneratedImages(asset);
           if( asset.get("editstatus") != "7" )
           {
			   asset.setProperty("editstatus", "7");
			   tosave.add(asset);
           }
		}
		else
		{
			existed++;
//            if( asset.get("editstatus") != "7" )
//            {
//			   asset.setProperty("editstatus", "6"); //restore files
//			   tosave.add(asset);
//            }
		}
		if( tosave.size() == 100 )
		{
			log.info("removed " + removed + " found " + existed);
			archive.saveAssets(tosave);
			tosave.clear();
		}
	}
	archive.saveAssets(tosave);
	tosave.clear();
	log.info("removed " + removed + " found " + existed);
	
}


init();
