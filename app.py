from fastapi import FastAPI, UploadFile, File, Header, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
import shutil
import os
from pathlib import Path
from typing import Optional
import requests
from parse_word import parse_word_file, generate_sql, connect_to_database, create_database_table

app = FastAPI()

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源，实际生产环境应该限制
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 创建上传目录
UPLOAD_DIR = Path("input")
UPLOAD_DIR.mkdir(exist_ok=True)

# 微信小程序配置
WX_CONFIG = {
    'appid': 'your_appid',
    'secret': 'your_secret'
}

async def get_current_user(token: str = Header(...)):
    """验证用户Token（微信openid）"""
    conn = None
    cursor = None
    try:
        conn = connect_to_database()
        cursor = conn.cursor(dictionary=True)
        
        # 查询用户
        cursor.execute("SELECT * FROM users WHERE openid = %s", (token,))
        user = cursor.fetchone()
        
        if not user:
            raise HTTPException(status_code=401, detail="未授权的访问")
            
        return user
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

@app.post("/login")
async def login(code: str):
    """微信登录"""
    try:
        # 获取openid
        url = f"https://api.weixin.qq.com/sns/jscode2session"
        params = {
            'appid': WX_CONFIG['appid'],
            'secret': WX_CONFIG['secret'],
            'js_code': code,
            'grant_type': 'authorization_code'
        }
        response = requests.get(url, params=params)
        data = response.json()
        
        if 'openid' not in data:
            return {"status": "error", "message": "登录失败"}
            
        openid = data['openid']
        
        # 查询或创建用户
        conn = connect_to_database()
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("SELECT * FROM users WHERE openid = %s", (openid,))
        user = cursor.fetchone()
        
        if not user:
            cursor.execute(
                "INSERT INTO users (openid) VALUES (%s)",
                (openid,)
            )
            conn.commit()
            cursor.execute("SELECT * FROM users WHERE openid = %s", (openid,))
            user = cursor.fetchone()
            
        return {
            "status": "success",
            "data": {
                "token": openid,  # 使用openid作为token
                "user": user
            }
        }
        
    except Exception as e:
        return {"status": "error", "message": str(e)}

@app.post("/upload")
async def upload_file(
    file: UploadFile = File(...),
    current_user: dict = Depends(get_current_user)
):
    """上传Word文件并解析入库"""
    try:
        # 保存上传的文件
        file_path = UPLOAD_DIR / file.filename
        with file_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # 解析Word文件
        questions = parse_word_file(str(file_path))
        
        # 修改SQL生成，添加user_id
        sql_statements = []
        for question in questions:
            options_str = None
            if question['type'] in ['单选题', '多选题']:
                options_str = '|||'.join(question['options'])
                
            sql = """INSERT INTO questions 
                     (user_id, question_type, question_text, options, correct_answer)
                     VALUES (%s, %s, %s, %s, %s);"""
            values = (
                current_user['id'],
                question['type'],
                question['text'],
                options_str,
                question['correct_answer']
            )
            sql_statements.append((sql, values))
            
        # 数据库操作
        conn = None
        cursor = None
        try:
            conn = connect_to_database()
            cursor = conn.cursor()
            
            for sql, values in sql_statements:
                cursor.execute(sql, values)
            
            conn.commit()
            
            return {
                "status": "success",
                "message": f"成功解析并导入 {len(questions)} 个题目",
                "data": {
                    "total_questions": len(questions)
                }
            }
            
        finally:
            if cursor:
                cursor.close()
            if conn:
                conn.close()
            
            # 清理上传的文件
            os.remove(file_path)
            
    except Exception as e:
        return {
            "status": "error",
            "message": str(e)
        }

@app.get("/questions")
async def get_questions(
    question_type: Optional[str] = None,
    page: int = 1,
    page_size: int = 10,
    current_user: dict = Depends(get_current_user)
):
    """查询题目列表"""
    try:
        conn = connect_to_database()
        cursor = conn.cursor(dictionary=True)
        
        # 构建查询条件
        where_clause = "WHERE user_id = %s"
        params = [current_user['id']]
        
        if question_type:
            where_clause += " AND question_type = %s"
            params.append(question_type)
        
        # 计算总数
        count_sql = f"SELECT COUNT(*) as total FROM questions {where_clause}"
        cursor.execute(count_sql, params)
        total = cursor.fetchone()['total']
        
        # 查询数据
        offset = (page - 1) * page_size
        sql = f"""
            SELECT * FROM questions 
            {where_clause}
            ORDER BY id DESC
            LIMIT %s OFFSET %s
        """
        cursor.execute(sql, params + [page_size, offset])
        questions = cursor.fetchall()
        
        return {
            "status": "success",
            "data": {
                "total": total,
                "page": page,
                "page_size": page_size,
                "questions": questions
            }
        }
        
    except Exception as e:
        return {
            "status": "error",
            "message": str(e)
        }
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

@app.put("/user")
async def update_user(
    nickname: Optional[str] = None,
    avatar_url: Optional[str] = None,
    current_user: dict = Depends(get_current_user)
):
    """更新用户信息"""
    try:
        conn = connect_to_database()
        cursor = conn.cursor()
        
        updates = []
        values = []
        if nickname:
            updates.append("nickname = %s")
            values.append(nickname)
        if avatar_url:
            updates.append("avatar_url = %s")
            values.append(avatar_url)
            
        if updates:
            sql = f"UPDATE users SET {', '.join(updates)} WHERE id = %s"
            values.append(current_user['id'])
            cursor.execute(sql, values)
            conn.commit()
            
        return {"status": "success"}
        
    except Exception as e:
        return {"status": "error", "message": str(e)}

@app.get("/init-db")
async def init_database():
    """初始化数据库表"""
    try:
        create_database_table()
        return {
            "status": "success",
            "message": "数据库表创建成功"
        }
    except Exception as e:
        return {
            "status": "error",
            "message": str(e)
        }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000) 