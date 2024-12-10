import os
from docx import Document
import mysql.connector
from pathlib import Path

# 创建input目录
input_dir = Path('input')
input_dir.mkdir(exist_ok=True)


def connect_to_database():
    """连接到MySQL数据库"""
    try:
        print("正在尝试连接数据库...")
        
        # 使用URL格式连接
        conn_str = (
            f"mysql+mysqlconnector://"
            f"questions:db5KDBD3G8DeJ3Y6@"
            f"2.tcp.vip.cpolar.cn:11085/"
            f"questions"
        )
        print(f"连接字符串: {conn_str}")
        
        conn = mysql.connector.connect(
            host="2.tcp.vip.cpolar.cn",
            port=11085,
            user="questions",
            password="db5KDBD3G8DeJ3Y6",
            database="questions",
            connect_timeout=10,
            allow_local_infile=True,
            use_pure=True
        )
        
        if conn.is_connected():
            print("数据库连接成功")
            return conn
            
        print("连接失败：无法建立连接")
        return None
            
    except mysql.connector.Error as err:
        print("\n数据库连接失败:")
        print(f"错误类型: {type(err)}")
        print(f"错误信息: {str(err)}")
        print(f"错误代码: {getattr(err, 'errno', 'N/A')}")
        print(f"SQL状态: {getattr(err, 'sqlstate', 'N/A')}")
        raise

def parse_word_file(file_path):
    """解析Word文件内容"""
    doc = Document(file_path)
    questions = []
    current_question = None
    current_type = None
    
    print("开始解析文件...")  # 调试信息
    
    for paragraph in doc.paragraphs:
        text = paragraph.text.strip()
        if not text:
            continue
            
        print(f"正在处理文本: {text}")  # 调试信息
            
        # 识别题目类型和题号
        if text.startswith('一、') or text.startswith('二、') or text.startswith('三、'):
            current_type = text.split(' ')[1]  # 获取题型
            print(f"识别到题型: {current_type}")  # 调试信息
            continue
            
        # 识别题目
        if text[0].isdigit() and '、' in text:
            if current_question:
                questions.append(current_question)
                print(f"添加题目: {current_question}")  # 调试信息
            
            question_text = text.split('、', 1)[1].strip()
            current_question = {
                'type': current_type,
                'text': question_text,
                'options': [],
                'correct_answer': ''
            }
            print(f"创建新题目: {question_text}")  # 调试信息
        
        # 处理选项
        elif text.startswith(('A.', 'B.', 'C.', 'D.')):
            if current_question:
                current_question['options'].append(text.strip())
                print(f"添加选项: {text.strip()}")  # 调试信息
            
        # 处理答案
        elif text.startswith('正确答案：'):
            if current_question:
                current_question['correct_answer'] = text.replace('正确答案：', '').strip()
                print(f"添加答案: {current_question['correct_answer']}")  # 调试信息
                questions.append(current_question)
                current_question = None
    
    print(f"解析完成，共有 {len(questions)} 个题目")  # 调试信息
    for q in questions:  # 调试信息
        print(f"题目类型: {q['type']}")
        print(f"题目内容: {q['text']}")
        print(f"选项: {q['options']}")
        print(f"答案: {q['correct_answer']}")
        print("---")
    
    return questions

def generate_sql(questions):
    """生成SQL插入语句"""
    sql_statements = []
    for question in questions:
        options_str = None
        if question['type'] in ['单选题', '多选题']:
            options_str = '|||'.join(question['options'])
            
        sql = """INSERT INTO questions (question_type, question_text, options, correct_answer)
                 VALUES (%s, %s, %s, %s);"""
        values = (
            question['type'],
            question['text'],
            options_str,
            question['correct_answer']
        )
        sql_statements.append((sql, values))
    return sql_statements

def create_database_table():
    """执行create_table.sql文件创建数据库表"""
    try:
        print("正在创建数据库表...")
        conn = connect_to_database()
        cursor = conn.cursor()
        
        # 读取SQL文件
        with open('create_table.sql', 'r', encoding='utf-8') as f:
            sql_commands = f.read()
        
        # 执行SQL命令
        for command in sql_commands.split(';'):
            if command.strip():
                cursor.execute(command + ';')
                print("执行SQL命令:", command.strip())
        
        conn.commit()
        print("数据库表创建成功")
        
    except mysql.connector.Error as err:
        print(f"创建表失败: {err}")
        if conn:
            conn.rollback()
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

def main():
    # 首先创建数据库表
    create_database_table()
    
    # 检查input目录中的Word文件
    files = os.listdir('input')
    print(f"在input目录中找到的文件: {files}")
    
    for file in files:
        # 跳过临时文件和隐藏文件
        if file.startswith('~$') or file.startswith('.'):
            print(f"跳过临时文件: {file}")
            continue
            
        if file.endswith('.docx'):
            file_path = os.path.join('input', file)
            print(f"正在处理文件: {file_path}")
            
            try:
                # 解析Word文件
                questions = parse_word_file(file_path)
                print(f"解析到 {len(questions)} 个问题")
                
                # 生成SQL语句
                sql_statements = generate_sql(questions)
                print(f"生成了 {len(sql_statements)} 条SQL语句")
                
                # 数据库操作
                conn = None
                cursor = None
                try:
                    conn = connect_to_database()
                    cursor = conn.cursor()
                    
                    for i, (sql, values) in enumerate(sql_statements):
                        cursor.execute(sql, values)
                        print(f"成功插入第 {i+1} 条数据")
                    
                    conn.commit()
                    print(f"成功处理文件 {file} 并插入数据库")
                    
                except mysql.connector.Error as err:
                    print(f"数据库错误: {err}")
                    if conn:
                        conn.rollback()
                finally:
                    if cursor:
                        cursor.close()
                    if conn:
                        conn.close()
                        
            except Exception as e:
                print(f"处理文件 {file} 时出错: {str(e)}")
                continue

if __name__ == "__main__":
    main() 