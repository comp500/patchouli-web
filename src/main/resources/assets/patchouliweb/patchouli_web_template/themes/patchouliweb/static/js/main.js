let items = document.getElementsByClassName("multiitem");
window.setInterval(() => {
	for (let item of items) {
		let list = Array.from(item.childNodes).filter(child => {
			return child.classList != null && child.classList.contains("itemstack");
		});
		if (list.length > 1) {
			let index = list.findIndex(child => {
				if (child.style.display != "none") {
					child.style.display = "none";
					return true
				}
				return false
			});
			if ((index + 1) >= list.length) {
				index = -1;
			}
			list[index + 1].style.display = "inline-block";
		}
	}
}, 1000);

let mainTooltip = document.createElement("span");
mainTooltip.style.display = "none";
mainTooltip.style.position = "absolute";
document.body.appendChild(mainTooltip);

let previousTarget = null;
let previousHTML = null;
let recalcTooltip = (e) => {
	/*tooltip.style.left =
		(e.pageX + tooltip.clientWidth + 10 < document.body.clientWidth)
		? (e.pageX + 10 + "px")
		: (document.body.clientWidth + 5 - tooltip.clientWidth + "px");
	tooltip.style.top =
		(e.pageY + tooltip.clientHeight + 10 < document.body.clientHeight)
		? (e.pageY + 10 + "px")
		: (document.body.clientHeight + 5 - tooltip.clientHeight + "px");*/
	// tooltip.style.left =
	// 	(e.clientX + tooltip.clientWidth + 10 < document.documentElement.clientWidth)
	// 	? (e.clientX + window.scrollX + 10 + "px")
	// 	: (document.documentElement.clientWidth + window.scrollX - 5 - tooltip.clientWidth + "px");
	// tooltip.style.top =
	// 	(e.clientY + tooltip.clientHeight + 10 < document.documentElement.clientHeight)
	// 	? (e.clientY + window.scrollY + 10 + "px")
	// 	: (document.documentElement.clientHeight + window.scrollY - 5 - tooltip.clientHeight + "px");
	/*let rect = item.getBoundingClientRect()
	tooltip.style.left = e.clientX - rect.left + "px";
	tooltip.style.top = e.clientY - rect.top + "px";*/
	if (e.target != previousTarget) {
		previousTarget = e.target;
		for (let tooltip of e.target.childNodes) {
			if (tooltip.classList != null && tooltip.classList.contains("itemstack-tooltip")) {
				if (tooltip.innerHTML != previousHTML) {
					previousHTML = tooltip.innerHTML;
					mainTooltip.innerHTML = previousHTML;
				}
				break;
			}
		}
	}
	mainTooltip.style.left =
		(e.clientX + mainTooltip.clientWidth + 10 < document.documentElement.clientWidth)
		? (e.clientX + window.scrollX + 10 + "px")
		: (document.documentElement.clientWidth + window.scrollX - 5 - mainTooltip.clientWidth + "px");
	mainTooltip.style.top =
		(e.clientY + mainTooltip.clientHeight + 10 < document.documentElement.clientHeight)
		? (e.clientY + window.scrollY + 10 + "px")
		: (document.documentElement.clientHeight + window.scrollY - 5 - mainTooltip.clientHeight + "px");
	mainTooltip.style.display = "inline-block";
};

for (let item of document.getElementsByClassName("itemstack")) {
	/*for (let tooltip of item.childNodes) {
		if (tooltip.classList != null && tooltip.classList.contains("itemstack-tooltip")) {
			
			break;
		}
	}*/
	item.addEventListener("mousemove", recalcTooltip, false);
	item.addEventListener("mouseenter", recalcTooltip, false);
	item.addEventListener("mouseleave", () => {
		mainTooltip.style.display = "none";
	}, false);
}