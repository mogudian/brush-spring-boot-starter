var ${table_name_upper}Vars = {};

${table_name_upper}Vars.baseURL = '${base_url}&method=';

${table_name_upper}Vars.columns = [${columns}];

${table_name_upper}Vars.storeFields = new Array();
${table_name_upper}Vars.cmColumns = new Array();

${table_name_upper}Vars.templateObj = {};

for (var i = 0, len = ${table_name_upper}Vars.columns.length; i < len; i++) {
  ${table_name_upper}Vars.storeFields.push(${table_name_upper}Vars.columns[i].dataIndex);
  if (${table_name_upper}Vars.columns[i].dataIndex != 'id') {
    ${table_name_upper}Vars.cmColumns.push(${table_name_upper}Vars.columns[i]);
    ${table_name_upper}Vars.templateObj[${table_name_upper}Vars.columns[i].dataIndex] = '';
  }
}

${table_name_upper}Vars.pageSize = ${page_size};

var ${table_name_lower}Store = new Ext.data.JsonStore({
  baseParams: { limit: ${table_name_upper}Vars.pageSize },
  totalProperty: 'totalCount',
  proxy: new Ext.data.HttpProxy({
    url: ${table_name_upper}Vars.baseURL + 'list'
  }),
  root: 'list',
  fields: ${table_name_upper}Vars.storeFields
});

${table_name_upper}GridPanel = Ext.extend(Ext.grid.GridPanel, {
  constructor: function () {
    var ${table_name_lower}EditWin = null;

    var sm = new Ext.grid.CheckboxSelectionModel({
      getEditor: Ext.emptyFn
    });

    var search = new Ext.ux.form.SearchField({
      id: '${table_name_lower}SearchField',
      store: ${table_name_lower}Store,
      width: 150
    });

    //监听保存按钮
    var editor = new Ext.ux.grid.RowEditor({
      saveText: '保存',
      cancelText: '取消',
      errorText: '错误',
      commitChangesText: '请先点击“保存”按钮来保存已经更改的内容',
      showTooltip: Ext.emptyFn,
      listeners: {
        canceledit: function(roweditor, pressed) {
          if (pressed) {
            if (!sm.getSelected().get('id')) {
              ${table_name_lower}Store.remove(sm.getSelected());
            }
          }
        },
        afteredit: function(roweditor, changes, record, rowIndex) {
          var id = record.get('id');
          var params = new Object();
          var method;
          if (id) {
            method = 'modify';
            params['id'] = id;
            for (var key in changes) {
              params[key] = changes[key];
            }
          } else {
            method = 'add';
            params = changes;
          }
          Ext.Ajax.request({
            url: ${table_name_upper}Vars.baseURL + method,
            params: params,
            method: 'post',
            async: false,
            success: function (response, opts) {
              ${table_name_lower}Store.reload();
              var json = Ext.util.JSON.decode(response.responseText);
              Ext.MessageBox.alert('成功', json.msg);
            },
            failure: function (response, opts) {
              var json = Ext.util.JSON.decode(response.responseText);
              Ext.Msg.alert('错误', json.msg);
            }
          });
        }
      }
    });

    var cm = new Ext.grid.ColumnModel({
      defaults: {
        sortable: true
      },
      columns: [sm].concat(${table_name_upper}Vars.cmColumns)
    });

    ${table_name_upper}GridPanel.superclass.constructor.call(this, {
      title: '${table_comment}',
      tbar: ['关键字：', search, '-', {
        text: '添加',
        cls: 'x-btn-text-icon',
        iconCls: 'icon-add',
        handler: function(btn) {
          //var e = new Ext.data.Record({${entity_template}});
          var e = new Ext.data.Record(${table_name_upper}Vars.templateObj);
          editor.stopEditing();
          ${table_name_lower}Store.insert(0, e);
          //this.getView().refresh();
          sm.selectRow(0);
          editor.startEditing(0);
        }
      }, '-', {
        text: '删除',
        cls: 'x-btn-text-icon',
        iconCls: 'icon-delete',
        handler: function (btn) {
          editor.stopEditing();
          if (sm.getCount() <= 0) {
            Ext.MessageBox.alert('系统提示', '请至少选择一条记录');
            return;
          }
          var selected = false;
          sm.each(function(record) {
            if (record.get('id')) {
              selected = true;
            }
          });
          if (!selected) {
            Ext.MessageBox.alert('系统提示', '请至少选择一条记录');
            return;
          }
          Ext.MessageBox.confirm('系统提示', '是否删除这些记录', function(b) {
            if (b == 'yes') {
              var ids = new Array();
              sm.each(function(record) {
                ids.push(record.get('id'));
              });
              Ext.Ajax.request({
                url: ${table_name_upper}Vars.baseURL + 'remove',
                params: { id: ids },
                method: 'post',
                async: false,
                success: function (response, opts) {
                  //${table_name_lower}Store.reload();
                  var rows = sm.getSelections();
                  for (var i = rows.length - 1; i >= 0; i--) {
                    ${table_name_lower}Store.remove(rows[i]);
                  }
                  /*
                  for (var i = 0, r; r = rows[i]; i++){
                    ${table_name_lower}Store.remove(r);
                  }
                  */
                  var json = Ext.util.JSON.decode(response.responseText);
                  Ext.MessageBox.alert('成功', json.msg);
                },
                failure: function (response, opts) {
                  var json = Ext.util.JSON.decode(response.responseText);
                  Ext.Msg.alert('错误', json.msg);
                }
              });
            }
          });
        }
      }],
      sm: sm,
      cm: cm,
      store: ${table_name_lower}Store,
      plugins: [editor],
      enableColumnHide: false,
      border: false,
      loadMask: true,
      stripeRows: true,
      viewConfig: {
        forceFit: true
      },
      bbar: new Ext.PagingToolbar({
        pageSize: ${table_name_upper}Vars.pageSize,
        store: ${table_name_lower}Store,
        displayInfo: true,
        beforePageText: '第',
        afterPageText: '/{0}页',
        firstText: '首页',
        prevText: '上一页',
        nextText: '下一页',
        lastText: '尾页',
        displayMsg: '显示 第 {0} 条 到 第 {1} 条 记录，共 {2} 条记录',
        emptyMsg: '无记录',
        plugins: [new Ext.ux.grid.PageSizePlugin({ region: 'right' })]
      }),
      listeners: {
        afterrender: function(cmp) {
          ${table_name_lower}Store.load({
            params: {
              start: 0
            }
          });
        }
      }
    });
  }
});
