package com.tisawesomeness.minecord.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.tisawesomeness.minecord.Config;

public class Database {
	
	private static Connection connect = null;
	private static ArrayList<DbGuild> guilds = new ArrayList<DbGuild>();
	private static ArrayList<DbUser> users = new ArrayList<DbUser>();
	private static int goal = 0;
	
	public static void init() throws SQLException {
		
		//Build database url
		String url = "jdbc:";
		if (Config.getType().equals("mysql")) {
			url += "mysql://";
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
				ex.printStackTrace();
			}
		} else {
			url += "sqlite:";
		}
		url += Config.getHost();
		if (Config.getType().equals("mysql")) {
			url += ":" + Config.getPort() + "/" + Config.getDbName() + "?autoReconnect=true&useSSL=false";
		}
		
		//Connect to database
		connect = DriverManager.getConnection(url, Config.getUser(), Config.getPass());
		
		//Create tables if they do not exist
		connect.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS guild (" +
			"  id BIGINT(18) NOT NULL," +
			"  prefix TINYTEXT NOT NULL," +
			"  banned TINYINT(1) NOT NULL DEFAULT 0," +
			"  noCooldown TINYINT(1) NOT NULL DEFAULT 0," +
			"  PRIMARY KEY (id));"
		);
		connect.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS user (" +
			"  id BIGINT(18) NOT NULL," +
			"  elevated TINYINT(1) NOT NULL DEFAULT 0," +
			"  banned TINYINT(1) NOT NULL DEFAULT 0," +
			"  upvoted TINYINT(1) NOT NULL DEFAULT 0," +
			"  PRIMARY KEY (id));"
		);
		connect.createStatement().executeUpdate(
			"CREATE TABLE IF NOT EXISTS goal (" +
			"  id INT NOT NULL," +
			"  last INT NOT NULL);"
		);
		
		replaceElevated(Long.valueOf(Config.getOwner()), true); //Add owner to elevated
		
		//Import guild list
		ResultSet rs = connect.createStatement().executeQuery(
			"SELECT * FROM guild;"
		);
		while (rs.next()) {
			guilds.add(new DbGuild(
				rs.getLong(1),
				rs.getString(2),
				rs.getBoolean(3),
				rs.getBoolean(4)
			));
		}
		rs.close();
		
		//Import user list
		rs = connect.createStatement().executeQuery(
			"SELECT * FROM user;"
		);
		while (rs.next()) {
			users.add(new DbUser(
				rs.getLong(1),
				rs.getBoolean(2),
				rs.getBoolean(3),
				rs.getBoolean(4)
			));
		}
		rs.close();
		
		//Import current goal
		rs = connect.createStatement().executeQuery(
			"SELECT last FROM goal;"
		);
		while (rs.next()) {
			int latest = rs.getInt(1);
			if (latest > goal) goal = latest;
		}
		
	}
	
	public static void changePrefix(long id, String prefix) throws SQLException {
		
		//If prefix is being reset to default
		if (prefix.equals(Config.getPrefix())) {
			
			//If the guild has not been banned or has no cooldown
			PreparedStatement st = connect.prepareStatement(
				"SELECT banned, noCooldown FROM guild WHERE id = ?;"
			);
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			rs.next();
			if (!rs.getBoolean(1) && !rs.getBoolean(2)) {
				
				//Delete guild
				st = connect.prepareStatement(
					"DELETE FROM guild WHERE id = ?;"
				);
				st.setLong(1, id);
				st.executeUpdate();
				
				guilds.remove(getGuild(id)); //Mirror change locally
				
			} else {
				
				replacePrefix(id, prefix); //Update guild with default prefix
				getGuild(id).prefix = prefix; //Locally change guild prefix to default
				
			}
			
		} else {
			
			replacePrefix(id, prefix); //Update guild with new prefix
			
			//Mirror change in local guild list
			DbGuild g = getGuild(id);
			if (g == null) {
				guilds.add(new DbGuild(id, prefix, false, false));
			} else {
				g.prefix = prefix;
			}
			
		}
		
	}
	
	private static void replacePrefix(long id, String prefix) throws SQLException {
		PreparedStatement st = connect.prepareStatement(
			"REPLACE INTO guild (id, prefix) VALUES(?, ?);"
		);
		st.setLong(1, id);
		st.setString(2, prefix);
		st.executeUpdate();
	}
	
	public static String getPrefix(long id) {
		DbGuild guild = Database.getGuild(id);
		return guild == null ? Config.getPrefix() : guild.prefix;
	}
	
	public static DbGuild getGuild(long id) {
		for (DbGuild g : guilds) {
			if (g.id == id) {
				return g;
			}
		}
		return null;
	}
	
	public static void changeElevated(long id, boolean elevated) throws SQLException {
		
		if (!elevated) {
			
			//If the user has not been banned or has upvoted
			PreparedStatement st = connect.prepareStatement(
				"SELECT banned, upvoted FROM user WHERE id = ?;"
			);
			st.setLong(1, id);
			ResultSet rs = st.executeQuery();
			rs.next();
			if (!rs.getBoolean(1) && !rs.getBoolean(2)) {
				
				//Delete user
				st = connect.prepareStatement(
					"DELETE FROM user WHERE id = ?;"
				);
				st.setLong(1, id);
				st.executeUpdate();
				
				users.remove(getUser(id)); //Mirror change locally
				
			} else {
				
				replaceElevated(id, false); //Demote user in database
				getUser(id).elevated = false; //Demote user locally
				
			}
			
		} else {
			
			replaceElevated(id, true); //Elevate user in database
			
			//Mirror change in local user list
			DbUser u = getUser(id);
			if (u == null) {
				users.add(new DbUser(id, true, false, false));
			} else {
				u.elevated = true;
			}
			
		}
		
	}
	
	private static void replaceElevated(long id, boolean elevated) throws SQLException {
		PreparedStatement st = connect.prepareStatement(
			"REPLACE INTO user (id, elevated) VALUES(?, ?);"
		);
		st.setLong(1, id);
		st.setBoolean(2, elevated);
		st.executeUpdate();
	}
	
	public static boolean isElevated(long id) {
		DbUser user = Database.getUser(id);
		return user == null ? false : user.elevated;
	}
	
	public static DbUser getUser(long id) {
		for (DbUser u : users) {
			if (u.id == id) {
				return u;
			}
		}
		return null;
	}
	
	public static int getGoal() {return goal;}

}
