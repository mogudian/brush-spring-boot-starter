var EditArea_f9= {
	onkeydown: function(e) {
		if (e.keyCode == 120) {
			if (window.executeQueryOrUpdate) {
				window.executeQueryOrUpdate(e);
			} else if (window.parent.executeQueryOrUpdate) {
				window.parent.executeQueryOrUpdate(e);
			}
		}
		return true;
	}
};
editArea.add_plugin("f9", EditArea_f9);