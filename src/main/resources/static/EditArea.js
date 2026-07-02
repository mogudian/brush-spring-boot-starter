Ext.namespace('Ext.ux.form');

Ext.ux.form.EditArea = Ext.extend(Ext.form.TextArea, {
    initComponent: function() {
        this.eaid = this.id;
        Ext.ux.form.EditArea.superclass.initComponent.apply(this, arguments);
        this.on('resize', function(ta, width, height) {
            var el = Ext.get('frame_editarea-' + this.eaid);
            if (el) {
                el.setSize(width,height);
                    //var size = dialog.getSize();
                    //el.setSize(size.width - dialog.getFrameWidth(), size.height - dialog.getFrameHeight());
            }
            
        });
        this.on('beforehide', function() {
            editAreaLoader.deleteInstance(this.eaid);
        });
    },
    onRender: function() {
    	// alert('onRender');
        Ext.ux.form.EditArea.superclass.onRender.apply(this, arguments);
        this.initEditor(false);
    },
    initEditor: function(b) {
    	editAreaLoader.init({
        id: this.eaid,
        start_highlight: true	// if start with highlight
  			,allow_resize: "both"
  			,allow_toggle: false
  			,language: "zh"
  			,syntax: "tsql"
  			,replace_tab_by_spaces: 2
  			,fullscreen: b
	    });
    },
    getValue: function() {
        var v = editAreaLoader.getValue(this.eaid);
        // this should set the textarea's value and fire events
        Ext.ux.form.EditArea.superclass.setValue.call(this, v);
        return v;
    },
    setValue: function(v) {
        Ext.ux.form.EditArea.superclass.setValue.call(this, v);
        editAreaLoader.setValue(this.eaid, v);
    },
    getSelectedText: function() {
    	editAreaLoader.getSelectedText(this.eaid);
    },
    getSelectionRange: function(callback) {
    	var sel = editAreaLoader.getSelectionRange(this.eaid);
    	callback(sel['start'], sel['end']);
    },
    setSize: function(width, height) {
    	var el = Ext.get('frame_editarea-' + this.eaid);
      if (el) {
      	alert('found');
        el.setSize(width, height);
      }
    },
    validate: function() {
        this.getValue();
        Ext.ux.form.EditArea.superclass.validate.apply(this, arguments);
    }
});

Ext.reg('ux-editarea', Ext.ux.form.EditArea);