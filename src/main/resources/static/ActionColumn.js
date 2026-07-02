Ext.ns("Ext.ux.grid");
/**
 * 结合rowAction和ActionColumn写的图标操作列
 * 示例：<pre><code>
 * 			{
					xtype: 'uxactioncolumn',
					header: '操作',
					autoWidth: false,
					width: 100,
					items: [{
					iconCls: 'icon-edit',
					tooltip: '示例',
					text: '示例',
					stopSelection: false,
					scope: this,
					handler: function(grid, rowIndex, colIndex) {
					}
				}
			</code></pre>
 * @class Ext.ux.grid.ActionColumn
 * @extends Ext.grid.ActionColumn
 */
Ext.ux.grid.ActionColumn = Ext.extend(Ext.grid.ActionColumn, {
  constructor: function(cfg) {
    var me = this,
        items = cfg.items || (me.items = [me]),
        l = items.length,
        i,
        item;

    Ext.grid.ActionColumn.superclass.constructor.call(me, cfg);

//      Renderer closure iterates through items creating an <img> element for each and tagging with an identifying 
//      class name x-action-col-{n}
    me.renderer = function(v, meta) {
//          Allow a configured renderer to create initial value (And set the other values in the "metadata" argument!)        	
      v = Ext.isFunction(cfg.renderer) ? cfg.renderer.apply(this, arguments)||'' : '';
      meta.css += ' x-action-col-cell';
      for (i = 0; i < l; i++) {
        item = items[i];

        v+='<div class="ux-action-col-item '+(item.iconCls||'')+' x-action-col-' + String(i) + ' '
            + (item.text? 'ux-action-col-text ':'')
            + (Ext.isFunction(item.getClass) ? item.getClass.apply(item.scope||this.scope||this, arguments) : '') + '"'
            +' style="{hide}{style}" '+ ((item.tooltip) ? ' ext:qtip="' + item.tooltip + '"' : '')+' >'
            + (item.text? '<span '+ ((item.tooltip) ? ' ext:qtip="' + item.tooltip + '"' : '')+'>'+item.text+'</span>':'')
            +'</div>';
      }
      return v;
    };
  },

//	constructor: function(cfg) {
//        var me = this,
//            items = cfg.items || (me.items = [me]),
//            l = items.length,
//            i,
//            item;
//
//        Ext.grid.ActionColumn.superclass.constructor.call(me, cfg);
//
////      Renderer closure iterates through items creating an <img> element for each and tagging with an identifying 
////      class name x-action-col-{n}
//        me.renderer = function(v, meta) {
////          Allow a configured renderer to create initial value (And set the other values in the "metadata" argument!)
//            v = Ext.isFunction(cfg.renderer) ? cfg.renderer.apply(this, arguments)||'' : '';
//
//            meta.css += ' x-action-col-cell';
//            for (i = 0; i < l; i++) {
//                item = items[i];
//                v += '<img alt="' + (item.altText || me.altText) + '" src="' + (item.icon || Ext.BLANK_IMAGE_URL) +
//                    '" class="x-action-col-icon x-action-col-' + String(i) + ' ' + (item.iconCls || '') +
//                    ' ' + (Ext.isFunction(item.getClass) ? item.getClass.apply(item.scope||this.scope||this, arguments) : '') + '"' +
//                    ((item.tooltip) ? ' ext:qtip="' + item.tooltip + '"' : '') + ' />';
//            }
//            return v;
//        };
//    },

  processEvent : function(name, e, grid, rowIndex, colIndex){
    var t = e.getTarget('.ux-action-col-item');
    if (t){
      var m = t.className.match(this.actionIdRe),item, fn;
      if (m && (item = this.items[parseInt(m[1], 10)])) {
        if (name == 'click') {
          (fn = item.handler || this.handler) && fn.call(item.scope||this.scope||this, grid, rowIndex, colIndex, item, e);
        } else if ((name == 'mousedown') && (item.stopSelection !== false)) {
          return false;
        }
      }
    }
    return Ext.grid.ActionColumn.superclass.processEvent.apply(this, arguments);
  }
});

Ext.apply(Ext.grid.Column.types, {
  uxactioncolumn : Ext.ux.grid.ActionColumn
});
