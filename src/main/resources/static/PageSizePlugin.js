/**
 * power by theboboy
 * 2012-12-11
 * 
 * 对Grid分页进行扩展，可以分页大小
 * 使用方法：
 * new Ext.PagingToolbar({
    pageSize: 20,
    store: store,
    displayInfo: true,
    plugins: [new Ext.ux.grid.PageSizePlugin()]
          或者[new Ext.ux.grid.PageSizePlugin({ region: 'center' })]
          或者[new Ext.ux.grid.PageSizePlugin({ region: 'right' })]
  });
 */

Ext.namespace('Ext.ux.grid');
Ext.ux.grid.PageSizePlugin = function(cfg) {
  this.region = 'left';
  this.prefixText = '每页';
  this.postfixText = '条';
  if (cfg) {
    Ext.apply(this, cfg);
  }
  Ext.ux.grid.PageSizePlugin.superclass.constructor.call(this, {
    store: new Ext.data.SimpleStore({
      fields: ['text', 'value'],
      data: [['5', 5], ['10', 10], ['15', 15], ['20', 20], ['25', 25], ['30', 30], ['50', 50], ['75', 75], ['100', 100]]
    }),
    mode: 'local',
    displayField: 'text',
    valueField: 'value',
    editable: false,
    allowBlank: false,
    triggerAction: 'all',
    width: 45
  });
};
Ext.extend(Ext.ux.grid.PageSizePlugin, Ext.form.ComboBox, {
  init: function(paging) {
    paging.on('render', this.onInitView, this);
  },
  onInitView: function(paging) {
    if (this.region == 'right') {
      paging.add('-', this.prefixText, this, this.postfixText);
    } else if (this.region == 'center') {
      var index = paging.displayInfo ? paging.items.length - 2 : paging.items.length;
      paging.insert(index, '-', this.prefixText, this, this.postfixText);
    } else {
      paging.insert(0, this.prefixText, this, this.postfixText);
    }
    this.setValue(paging.pageSize);
    this.on('select', this.onPageSizeChanged, paging);
  },
  onPageSizeChanged: function(combo) {
    this.pageSize = parseInt(combo.getValue());
    this.doLoad(0);
  }
});

Ext.preg('pageSizePlugin', Ext.ux.grid.PageSizePlugin);