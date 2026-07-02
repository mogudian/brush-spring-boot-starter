var ${table_name_upper}Vars = {};

${table_name_upper}Vars.baseURL = '${base_url}&method=';

//header, dataIndex, name(hiddenName), editor(xtype)
${table_name_upper}Vars.columns = [${columns}];

${table_name_upper}Vars.storeFields = new Array();
${table_name_upper}Vars.cmColumns = new Array();
${table_name_upper}Vars.formItems = new Array();

${table_name_upper}Vars.renderFn = function(value) {
  var query = Ext.getCmp('${table_name_lower}SearchField').getValue();
  if (query != '') {
    var index = value.indexOf(query);
    if (index >= 0) {
      var left = value.substring(0, index);
      var right = value.substring(index + query.length);
      return left + '<span style="background-color: yellow; color: red;">' + query + '</span>' + right;
    }
  }
  return value;
}

var columnXtypeMap = {};

for (var i = 0, len = ${table_name_upper}Vars.columns.length; i < len; i++) {
  ${table_name_upper}Vars.storeFields.push(${table_name_upper}Vars.columns[i].dataIndex);
  if (${table_name_upper}Vars.columns[i].dataIndex != 'id' || true) {
    ${table_name_upper}Vars.cmColumns.push({ header: ${table_name_upper}Vars.columns[i].header, dataIndex: ${table_name_upper}Vars.columns[i].dataIndex, renderer: ${table_name_upper}Vars.renderFn });
  }
  if (!${table_name_upper}Vars.columns[i].autoIncr) {
    ${table_name_upper}Vars.formItems.push(${table_name_upper}Vars.columns[i]);
  }
  columnXtypeMap[${table_name_upper}Vars.columns[i].dataIndex] = ${table_name_upper}Vars.columns[i].xtype;
}

${table_name_upper}Vars.pageSize = ${page_size};

var ${table_name_lower}Store = new Ext.data.GroupingStore({
  baseParams: { limit: ${table_name_upper}Vars.pageSize },
  proxy: new Ext.data.HttpProxy({
    url: ${table_name_upper}Vars.baseURL + 'list'
  }),
  reader: new Ext.data.JsonReader({
    totalProperty: 'totalCount',
    root: 'list',
    messageProperty: 'msg',
    fields: ${table_name_upper}Vars.storeFields
  })
});

${table_name_upper}EditWindow = Ext.extend(Ext.Window, {
  objId: null,
  edit: null,
  //add id=load id+edit=modify
  constructor : function(cfg) {
    Ext.apply(this, cfg);

    var opArr = ['查看', '添加', '修改'];
    var urlArr = ['view', 'add', 'modify'];

    var temp = (this.edit) ? 2 : (this.objId) ? 0 : 1;

    var op = opArr[temp];
    var url = ${table_name_upper}Vars.baseURL + urlArr[temp];

    for (var i = 0; i < ${table_name_upper}Vars.formItems.length; i++) {
      ${table_name_upper}Vars.formItems[i].disabled = !temp;
    }

    var formPan = new Ext.FormPanel({
      frame: true,
      baseCls: 'x-plain',
      bodyStyle: 'padding-top: 10px;',
      defaults: {
        xtype: 'textfield',
        width: 300
      },
      labelWidth: 200,
      items: ${table_name_upper}Vars.formItems
    });

    if (this.edit) {
      formPan.add({
        xtype: 'hidden',
        name: 'id'
      });
    }

    var btns = [{
      text: '关闭',
      handler: function(btn) {
        btn.ownerCt.ownerCt.close();
      },
      scope: this
    }];

    // 非查看操作
    if (temp > 0) {
      var me = this;
      btns = [{
        text : op,
        handler : function(btn) {
          formPan.getForm().submit({
            url: url,
            waitTitle : '系统提示',
            waitMsg : '正在提交，请稍等...',
            success : function(form, action) {
              if (action.result.success) {
                ${table_name_lower}Store.reload();
                if (me.edit) {
                  Ext.MessageBox.alert('成功', action.result.msg);
                  btn.ownerCt.ownerCt.close();
                } else {
                  Ext.MessageBox.confirm('成功', action.result.msg + '是否继续添加？', function(b) {
                    if (b != 'yes') {
                      btn.ownerCt.ownerCt.close();
                    }
                  });
                }
              } else {
                Ext.MessageBox.alert('失败', action.result.msg);
              }
            },
            failure : function(form, action) {
              Ext.MessageBox.alert('失败', action.result.msg);
            }
          });
        },
        scope: this
      }, {
        text : '清空',
        handler : function() {
          formPan.getForm().reset();
        }
      }]
    }

    ${table_name_upper}EditWindow.superclass.constructor.call(this, {
      title: op + '${table_comment}',
      modal: true,
      width: 700,
      // height: title+space+fields+space+button
      height: Math.min(25 + 30 + ${table_name_upper}Vars.formItems.length * 25 + 30 + 40, document.documentElement.clientHeight),
      autoScroll: true,
      resizable: false,
      plain: true,
      layout: 'column',
      items: [{
        columnWidth: .12,
        baseCls: 'x-plain',
        html: '&nbsp;'
      }, {
        columnWidth: .80,
        baseCls: 'x-plain',
        items: [formPan]
      }, {
        columnWidth: .8,
        baseCls: 'x-plain',
        html: '&nbsp;'
      }],
      buttons: btns,
      listeners: {
        afterrender: function(cmp) {
          if (this.objId) {
            var mask = new Ext.LoadMask(this.getId(), {
              msg: '正在加载...'
            });
            mask.show();
            var id = this.objId;
            Ext.Ajax.request({
              url: ${table_name_upper}Vars.baseURL + 'load',
              params: { id: id },
              success: function(response, opts) {
                var json = Ext.util.JSON.decode(response.responseText);
                var data = json.data;
                var map = new Object();
                for (var key in data) {
                  map[key] = data[key];
                  if (map[key] && columnXtypeMap[key]) {
                    if (columnXtypeMap[key] == 'datetimefield') {
                      map[key] = dayjs(map[key], 'YYYY-MM-DD HH:mm:ss').toDate();
                    }
                  }
                }
                formPan.getForm().setValues(map);
                mask.hide();
              },
              failure: function(response, opts) {
                //Ext.Msg.alert('错误', '数据加载失败');
                var json = Ext.util.JSON.decode(response.responseText);
                Ext.Msg.alert('错误', json.msg);
                mask.hide();
                cmp.close();
              }
            });
          }
        }
      }
    });
  }
});

${table_name_upper}GridPanel = Ext.extend(Ext.grid.GridPanel, {
  constructor: function () {
    var ${table_name_lower}EditWin = null;

    var sm = new Ext.grid.CheckboxSelectionModel({});

    var viewFn = function() {
      new ${table_name_upper}EditWindow({ objId: sm.getSelected().get('id') }).show();
    };

    var editFn = function (grid, row, col) {
      if (sm.getCount() == 0) {
        Ext.MessageBox.alert('系统提示', '请先选择一条记录');
        return;
      }
      new ${table_name_upper}EditWindow({ edit: true, objId: sm.getSelected().get('id') }).show();
    };

    var deleteFn = function (grid, row, col) {
      if (sm.getCount() != 1) {
        Ext.MessageBox.alert('系统提示', '只能选择一条记录');
        return;
      }
      Ext.MessageBox.confirm('系统提示', '是否删除这条记录', function (b) {
        if (b == 'yes') {
          Ext.Ajax.request({
            url: ${table_name_upper}Vars.baseURL + 'remove',
            params: { id: sm.getSelected().get('id') },
            async: false,
            success: function (response, opts) {
              ${table_name_lower}Store.reload();
              var json = Ext.util.JSON.decode(response.responseText);
              Ext.MessageBox.alert('成功', json.msg);
              //grid.getStore().remove(sm.getSelected());
            },
            failure: function (response, opts) {
              var json = Ext.util.JSON.decode(response.responseText);
              Ext.Msg.alert('错误', json.msg);
            }
          });
        }
      });
    };

    var recoverFn = function (grid, row, col) {
      alert('这个功能下期再做');
    };

    var rowBtns, rightMenu;

    if ('${table_name_lower}' === 'tBackupTable') {
      rowBtns = [{
        iconCls: 'icon-save',
        text: '恢复',
        tooltip: '恢复',
        stopSelection: false,
        scope: this,
        handler: recoverFn
      }];

      rightMenu = new Ext.menu.Menu({
        items: [{
          text: '查看',
          iconCls: 'icon-view',
          handler: viewFn
        }, '-', {
          iconCls: 'icon-save',
          text: '恢复',
          tooltip: '恢复',
          stopSelection: false,
          scope: this,
          handler: recoverFn
        }]
      });
    } else {
      rowBtns = [{
        iconCls: 'icon-edit',
        text: '编辑',
        tooltip: '编辑',
        stopSelection: false,
        scope: this,
        handler: editFn
      }, {
        iconCls: 'icon-delete',
        text: '删除',
        tooltip: '删除',
        stopSelection: false,
        scope: this,
        handler: deleteFn
      }];

      rightMenu = new Ext.menu.Menu({
        items: [{
          text: '查看',
          iconCls: 'icon-view',
          handler: viewFn
        }, '-', {
          text: '编辑',
          iconCls: 'icon-edit',
          handler: editFn
        }, {
          text: '删除',
          iconCls: 'icon-delete',
          handler: deleteFn
        }]
      });
    }

    var search = new Ext.ux.form.SearchField({
      id: '${table_name_lower}SearchField',
      store: ${table_name_lower}Store,
      width: 500,
      emptyText: 'where',
      errorCallback: function () {
        Ext.MessageBox.alert('错误', '查询条件错误，请检查');
        ${table_name_lower}Store.removeAll();
      }
    });

    var actions = {
      xtype: 'uxactioncolumn',
      header: '操作',
      autoWidth: false,
      width: rowBtns.length * 55,
      items: rowBtns
    };

    var tbar = ['查询条件：', search];

    if ('${table_name_lower}' !== 'tBackupTable') {
      tbar = tbar.concat(['-', {
        text: '添加',
        cls: 'x-btn-text-icon',
        iconCls: 'icon-new',
        handler: function(btn) {
          new ${table_name_upper}EditWindow().show();
        }
      }, '-', {
        text: '删除',
        cls: 'x-btn-text-icon',
        iconCls: 'icon-delete',
        handler: function (btn) {
          if (sm.getCount() <= 0) {
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
                  ${table_name_lower}Store.reload();
                  var json = Ext.util.JSON.decode(response.responseText);
                  Ext.MessageBox.alert('成功', json.msg);
                  /*var rows = sm.getSelections();
                  for (var i = rows.length - 1; i >= 0; i--) {
                    ${table_name_lower}Store.remove(rows[i]);
                  }*/
                },
                failure: function (response, opts) {
                  var json = Ext.util.JSON.decode(response.responseText);
                  Ext.Msg.alert('错误', json.msg);
                }
              });
            }
          });
        }
      }]);
    }

    var cm = new Ext.grid.ColumnModel({
      defaults: {
        sortable: true
      },
      columns: [sm].concat(${table_name_upper}Vars.cmColumns).concat([actions])
    });

    ${table_name_upper}GridPanel.superclass.constructor.call(this, {
      //title: '${table_comment}',
      tbar: tbar,
      sm: sm,
      cm: cm,
      store: ${table_name_lower}Store,
      enableColumnHide: false,
      border: false,
      loadMask: true,
      stripeRows: true,
      view: new Ext.grid.GroupingView({
        //forceFit: true,
        groupByText: '按此列分组',
        showGroupsText: '显示分组',
        groupTextTpl: '{text} ({[values.rs.length]} 条)'
      }),
      region: 'center',
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
        },
        dblclick: viewFn,
        rowcontextmenu: function(grid, rowIndex, e) {
          grid.getSelectionModel().selectRow(rowIndex);
          e.preventDefault();
          rightMenu.showAt(e.getXY());
        }
      }
    });
  }
});
