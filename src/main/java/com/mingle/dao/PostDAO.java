package com.mingle.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;

public class PostDAO {
	@Autowired
	private SqlSession db;
}
