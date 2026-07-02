/**
 * SQL 格式化器（带语法校验）
 * 基于 node-sql-parser + sql-formatter
 * 
 * 使用方式：
 * 1. 在 HTML 中引入依赖：
 *    <script src="https://cdnjs.cloudflare.com/ajax/libs/sql-formatter/15.6.9/sql-formatter.min.js"></script>
 *    <script src="https://unpkg.com/node-sql-parser/umd/mysql.umd.js"></script>
 * 
 * 2. 引入本文件：
 *    <script src="sql-formatter-with-validation.js"></script>
 * 
 * 3. 调用 formatSQL：
 *    formatSQL(
 *        "select * from users",
 *        function(formatted) { console.log(formatted); },
 *        function(error) { console.error(error); }
 *    );
 */

function validateSQL(sql) {
    const parser = new NodeSQLParser.Parser();
    try {
        parser.astify(sql);
        return null;
    } catch (parseError) {
        return '语法错误：' + parseError.message;
    }
}

/**
 * SQL格式化主函数
 * @param {string} sql - 需要格式化的SQL字符串
 * @param {Function} successCallback - 成功回调函数，接收格式化后的SQL字符串
 * @param {Function} errorCallback - 错误回调函数，接收错误信息
 * @param {Object} options - 可选配置
 * @param {string} options.keywordCase - 关键字大小写: 'preserve' | 'upper' | 'lower' (默认: 'preserve')
 * @param {string} options.indentStyle - 缩进风格: 'standard' | 'tabularLeft' | 'tabularRight' (默认: 'standard')
 * @param {string} options.database - 数据库类型: 'mysql' | 'postgresql' | 'sqlite' (默认: 'mysql')
 */
function formatSQL(sql, successCallback, errorCallback, options) {
    // 默认配置
    const defaultOptions = {
        keywordCase: 'preserve',
        indentStyle: 'standard',
        database: 'mysql'
    };

    // 合并配置
    const config = Object.assign({}, defaultOptions, options);

    // 参数校验
    if (!sql || typeof sql !== 'string') {
        if (typeof errorCallback === 'function') {
            errorCallback('SQL字符串不能为空');
        }
        return;
    }

    if (typeof successCallback !== 'function') {
        throw new Error('successCallback 必须是函数');
    }

    if (typeof errorCallback !== 'function') {
        throw new Error('errorCallback 必须是函数');
    }

    // 检查依赖库是否加载
    if (typeof NodeSQLParser === 'undefined') {
        errorCallback('缺少依赖：node-sql-parser 库未加载');
        return;
    }

    if (typeof sqlFormatter === 'undefined') {
        errorCallback('缺少依赖：sql-formatter 库未加载');
        return;
    }

    // 1. 语法校验
    let validateResult = validateSQL(sql);
    if (validateResult) {
        errorCallback(validateResult);
        return;
    }

    // 2. 格式化
    try {
        const formatConfig = {
            language: config.database,
            keywordCase: config.keywordCase,
            indentStyle: config.indentStyle,
            tabWidth: 2,
            useTabs: false
        };

        const formatted = sqlFormatter.format(sql, formatConfig);
        successCallback(formatted);
    } catch (formatError) {
        errorCallback('格式化错误：' + formatError.message);
    }
}

// 支持CommonJS模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { formatSQL };
}
