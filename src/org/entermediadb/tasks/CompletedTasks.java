package org.entermediadb.tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openedit.Data;
import org.openedit.MultiValued;

public class CompletedTasks
{
	protected List fieldCompletedTickets = new ArrayList();
	
	public List getCompletedTickets()
	{
		return fieldCompletedTickets;
	}
	public void setCompletedTickets(List inCompletedTickets)
	{
		fieldCompletedTickets = inCompletedTickets;
	}
	public CompletedTasks()
	{
		fieldCompleted = new ArrayList();
	}
	public CompletedTasks(List inCompleted)
	{
		fieldCompleted = inCompleted;
	}
	protected List fieldCompleted;
	
	public int weeksIn(Date month)
	{
//		GregorianCalendar completedweek = new GregorianCalendar();
//		completedweek.setTime(month);
		return 4;
	}
	
	public Collection getTicketsForWeek(int inweek)
	{
		List tickets = new ArrayList();
		for (Iterator iterator = getCompletedTickets().iterator(); iterator.hasNext();)
		{
			MultiValued ticket = (MultiValued) iterator.next();
			Date completedon = ticket.getDate("resolveddate");
			GregorianCalendar completedweek = new GregorianCalendar();
			completedweek.setTime(completedon);
			int week = completedweek.get(Calendar.WEEK_OF_YEAR);
			if( week == inweek)
			{
				tickets.add(ticket);
			}
		}
		return tickets;
	}
	
	public Collection byWeeks()
	{
		Map weeks = new HashMap();
		for (Iterator iterator = fieldCompleted.iterator(); iterator.hasNext();)
		{
			MultiValued task = (MultiValued) iterator.next();
			Date completedon = task.getDate("completedon");
			GregorianCalendar completedweek = new GregorianCalendar();
			completedweek.setTime(completedon);
			int week = completedweek.get(Calendar.WEEK_OF_YEAR);
			Collection saved = (Collection)weeks.get(week);
			if( saved == null)
			{
				saved = new ArrayList();
				weeks.put(week, saved);
			}
			saved.add(task);
		}
		List byweeks = new ArrayList(weeks.keySet());
		Collections.sort(byweeks);
		List sorted = new ArrayList();
		for (Iterator iterator = byweeks.iterator(); iterator.hasNext();)
		{
			Integer weekcount = (Integer) iterator.next();
			List tasks = (List)weeks.get(weekcount);
			Collections.reverse(tasks);
			sorted.add(tasks);
		}
		return sorted;
	}
	
	public void addTask(MultiValued inTask)
	{
		fieldCompleted.add(inTask);
	}
	public void addTicket(MultiValued inTicket)
	{
		fieldCompletedTickets.add(inTicket);
	}
	
}
