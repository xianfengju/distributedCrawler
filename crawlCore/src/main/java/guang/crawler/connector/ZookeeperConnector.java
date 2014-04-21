package guang.crawler.connector;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Transaction;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZookeeperConnector
{
	private ZooKeeper	zookeeper;
	
	public ZookeeperConnector(String connectString) throws IOException
	{
		this.zookeeper = new ZooKeeper(connectString, 3000, null);
	}
	
	public String checkAndCreateNode(String path, CreateMode createMode,
	        byte[] data) throws InterruptedException
	{
		boolean exists = false;
		if ((createMode == CreateMode.EPHEMERAL_SEQUENTIAL)
		        || (createMode == CreateMode.PERSISTENT_SEQUENTIAL))
		{
			try
			{
				exists = this.checkNodeExists(path);
			} catch (KeeperException e)
			{
				e.printStackTrace();
			}
		}
		if (!exists)
		{
			String realPath = this.createNode(path, createMode, data);
			return realPath;
		}
		return path;
	}
	
	private boolean checkNodeExists(String path) throws KeeperException,
	        InterruptedException
	{
		boolean nodeExists = false;
		Stat status = this.zookeeper.exists(path, null);
		nodeExists = (status != null);
		return nodeExists;
	}
	
	public String createNode(String path, CreateMode createMode, byte[] data)
	        throws InterruptedException
	{
		try
		{
			String realPath = this.zookeeper.create(path, data,
			        Ids.OPEN_ACL_UNSAFE, createMode);
			return realPath;
		} catch (KeeperException e)
		{
			
			e.printStackTrace();
			return null;
		}
		
	}
	
	public List<String> getChildren(String path) throws InterruptedException
	{
		try
		{
			return this.zookeeper.getChildren(path, false);
		} catch (KeeperException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public byte[] getData(String path) throws InterruptedException
	{
		try
		{
			byte[] data = this.zookeeper.getData(path, false, null);
			return data;
		} catch (KeeperException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public void moveTo(String fromPath, String toPath) throws KeeperException,
	        InterruptedException
	{
		Transaction transaction = this.zookeeper.transaction();
		LinkedList<String> mvPath = new LinkedList<>();
		mvPath.add("");
		while (!mvPath.isEmpty())
		{
			String first = mvPath.removeFirst();
			String path = fromPath + first;
			byte[] data = this.zookeeper.getData(path, false, null);
			transaction.create(toPath, data, Ids.OPEN_ACL_UNSAFE,
			        CreateMode.PERSISTENT);
			List<String> children = this.zookeeper.getChildren(path, false);
			if ((children != null) && (children.size() > 0))
			{
				for (String child : children)
				{
					mvPath.add(first + "/" + child);
				}
			}
		}
		this.recursiveDelete(fromPath, transaction);
		transaction.commit();
		
	}
	
	public boolean recursiveDelete(String path, Transaction transaction)
	        throws InterruptedException
	{
		
		try
		{
			List<String> children = this.zookeeper.getChildren(path, false);
			if (children.size() == 0)
			{
				this.simpleDelete(path, transaction);
				return true;
			} else
			{
				boolean success = true;
				for (String child : children)
				{
					success = this.recursiveDelete(path + "/" + child,
					        transaction);
					if (!success)
					{
						return false;
					}
				}
				this.simpleDelete(path, transaction);
				return true;
			}
		} catch (KeeperException e)
		{
			return false;
		}
	}
	
	public void shutdown() throws InterruptedException
	{
		this.zookeeper.close();
	}
	
	public boolean simpleDelete(String path, Transaction transaction)
	        throws InterruptedException
	{
		try
		{
			if (transaction != null)
			{
				transaction.delete(path, -1);
			} else
			{
				this.zookeeper.delete(path, -1);
			}
			return true;
		} catch (KeeperException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public Transaction transaction()
	{
		return this.zookeeper.transaction();
	}
}