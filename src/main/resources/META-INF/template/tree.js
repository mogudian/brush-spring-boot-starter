var ${db_name_upper}Vars = {};

${db_name_upper}Vars.baseURL = '${base_url}&method=';

${db_name_upper}Tree = Ext.extend(Ext.tree.TreePanel, {
  constructor: function () {
    var tree = this;

    var treeFilter = new Ext.tree.TreeFilter(tree, {
      clearBlank : true,
      autoClear : true
    });
    var timeoutId  = null;
    var hiddenPkgs = [];

    // 过滤不匹配的非叶子节点或者是叶子节点
    var judge =function(n,re) {
      var str = false;
      n.cascade(function (n1) {
        if (n1.isLeaf()) {
          if (re.test(n1.text)) {
            str = true;
            return;
          }
        } else {
          if (re.test(n1.text)) {
            str = true;
            return;
          }
        }
      });
      return str;
    }

    var findByKeyWordFiler = function (field, event) {
      // 清除timeout
      if (!!timeoutId) {
        clearTimeout(timeoutId);
      }
      tree.expandAll();// 展开树节点
      // 为了避免重复的访问后台，给服务器造成的压力，采用timeoutId进行控制，如果采用treeFilter也可以造成重复的keyup
      timeoutId = setTimeout(function () {
        // 获取输入框的值
        var text = field.getValue();
        // 根据输入制作一个正则表达式，'i'代表不区分大小写
        var re = new RegExp(Ext.escapeRe(text), 'i');
        // 先要显示上次隐藏掉的节点
        Ext.each(hiddenPkgs, function (n) {
          n.ui.show();
        });
        hiddenPkgs = [];
        if (text != "") {
          treeFilter.filterBy(function (n) {
            // 只过滤叶子节点，这样省去枝干被过滤的时候，底下的叶子都无法显示
            return !n.isLeaf() || re.test(n.text);
          });
          // 如果这个节点不是叶子，而且下面没有子节点，就应该隐藏掉
          tree.root.cascade(function (n) {
            if (n.id != '${db_name_lower}Root') {
              if (!n.isLeaf() && judge(n, re) == false && !re.test(n.text)) {
                hiddenPkgs.push(n);
                n.ui.hide();
              }
            }
          });
        } else {
          treeFilter.clear();
          return;
        }
      }, 100);
    };

    ${db_name_upper}Tree.superclass.constructor.call(this, {
      //title: '${db}',
      autoScroll: true,
      //autoHeight: true,
      border: false,
      useArrows: true,
      root: {
        nodeType: 'async',
        id: '${db_name_lower}Root',
        text: '${db}',
        expanded: true,
        uiProvider: false
      },
      rootVisible: false,
      loader: new Ext.tree.TreeLoader({
        dataUrl: ${db_name_upper}Vars.baseURL + 'list'
      }),
      tbar: ['搜索：', {
        xtype: 'textfield',
        id: 'filter_input',
        emptyText: '请输入关键字',
        enableKeyEvents: true,
        listeners: {
          keyup: findByKeyWordFiler
        }
      }],
      listeners: {
        afterrender: function(cmp) {
          cmp.expandAll();
        },
        dblclick: function(node, e) {
          if (node.leaf) {
            //Ext.Msg.alert('提示', '表名：' + node.id);
            
            var dbName = node.parentNode.text;
            
            var tabs = Ext.getCmp('centerTabs');
            
            if (!tabs) {
              return;
            }
            
            var tabId = 'center_tab_' + node.id;
            
            if (Ext.getCmp(tabId)) {
              tabs.setActiveTab(tabId);
              return;
            }
            
            Ext.Msg.wait('正在接收数据，请耐心等待...', '系统提示');
            
            ($).getJs('grid?db=' + dbName + '&table=' + node.id, function() {
              var arr = node.id.split('_');
                
              var name = '';
                
              for (var i = 0, len = arr.length; i < len; i++) {
                name += arr[i].charAt(0).toUpperCase() + arr[i].substr(1);
              }
                
              var grid = null;
                
              eval('grid = new ' + name + 'GridPanel();');
              
              grid.closable = true;
                
              var tab = new Ext.Panel({
                id: tabId,
                title: node.text,
                closable: true,
                layout: 'fit',
                items: [grid]
              });
              
              tabs.add(tab).show();
              
              Ext.Msg.hide();
            }, false);
          }
        }
      }
    });
  }
});

