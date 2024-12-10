// 配置API基础URL
const config = {
    baseURL: process.env.NODE_ENV === 'development' 
        ? 'http://localhost:8080'  // 开发环境
        : 'http://your_domain.com'  // 生产环境（替换为您的域名或IP）
};

export default config; 