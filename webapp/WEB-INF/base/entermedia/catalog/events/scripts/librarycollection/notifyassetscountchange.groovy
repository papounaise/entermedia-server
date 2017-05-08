package librarycollection;

import org.entermediadb.asset.MediaArchive
import org.entermediadb.email.WebEmail
import org.openedit.Data
import org.openedit.MultiValued
import org.openedit.data.Searcher
import org.openedit.hittracker.HitTracker
import org.openedit.profile.UserProfile
import org.openedit.users.User


public void init()
{
	MediaArchive mediaArchive = (MediaArchive)context.getPageValue("mediaarchive");
	
	Date quietperiod = new Date(System.currentTimeMillis() - 60*1000*10);
	
	Map collections = new HashMap();
	
	HitTracker found = mediaArchive.query("librarycollection").match("lastmodifieddatedirty","true").search();
	if( found.isEmpty() )
	{
		log.info("No edited collections");
		return;
	}
	for (MultiValued collection in found)
	{
		//Send notification?
		Date lastsent = collection.getDate("lastmodifieddatesent");
		if( lastsent == null || lastsent.before(quietperiod))
		{
			int counted = mediaArchive.query("asset").match("category",collection.get("rootcategory")).search().size();
			String oldcount = collection.getValue("assetcounted");
			if( oldcount== null || !oldcount.equals(String.valueOf(counted)))
			{ 
				collection = mediaArchive.getSearcher("librarycollection").loadData(collection);
				collection.setValue("assetcounted", counted);
				collections.put(collection.getId(), collection);
			}	
		}
	}
	if( collections.isEmpty() )
	{
		log.info("no collections edited or within 10 min");
		return;
	}
	//TODO: Add owners as followers automatically
	HitTracker followers = mediaArchive.query("librarycollectionshares").orgroup("librarycollection",collections.keySet()).search();
	if( followers.isEmpty() )
	{asset
		log.info("No followers on collection");
		return;
	}
	Date now = new Date();
	
	//Load each users collections
	Map users = new HashMap();
	for (MultiValued follower in followers)
	{
		String userid = follower.get("followeruser");
		List dirtycollections = (List)users.get(userid);
		if( dirtycollections == null)
		{
			dirtycollections = new ArrayList();
			users.put(userid, dirtycollections);
		}
		String collectionid = follower.get("librarycollection");
		dirtycollections.add(collections.get(collectionid));
	}
	
	for (String userid in users.keySet())
	{
		//Make sure the root folder is within the library root folder
		User followeruser = mediaArchive.getUserManager().getUser(userid);
		if( followeruser != null && followeruser.getEmail() != null)
		{
			//Save this in the user profile
			
			Data profile = mediaArchive.getData("userprofile", userid);
			if(profile.getBoolean("sendcollectionnotifications") == false)
			{
				log.info("Notification disabled " + userid);
				continue;	
			}
			String appid  = profile.get("preferedapp");
			if( appid == null)
			{
				appid =  mediaArchive.getCatalogSettingValue("events_notify_app");
			}	
			
			//collection-count-modifed
			String template = "/" + appid + "/theme/emails/collection-count-modified.html";
		
			List dirtycollections = (List)users.get(userid);
			
			WebEmail templatemail = mediaArchive.createSystemEmail(followeruser, template);
			templatemail.setSubject("[EM] " + dirtycollections.size + " Collection Notifications"); //TODO: Translate
			Map objects = new HashMap();
			objects.put("dirtycollections",dirtycollections);
			objects.put("followeruser",followeruser);
			objects.put("apphome","/" + appid);
			templatemail.send(objects);
		}
	}
	Searcher searcher = mediaArchive.getSearcher("librarycollection");
	List tosave = new ArrayList();
	for (MultiValued collection in found)
	{
		Data data = searcher.loadData(collection);
		data.setValue("lastmodifieddatedirty",false);
		data.setValue("lastmodifieddatesent",now);
		tosave.add(data);
	}
	searcher.saveAllData(tosave, null);
	
}

init();

