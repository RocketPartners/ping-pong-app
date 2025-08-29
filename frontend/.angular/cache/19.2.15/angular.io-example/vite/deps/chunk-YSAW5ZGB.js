import {
  MatPseudoCheckbox
} from "./chunk-FUD5P55H.js";
import {
  MatCommonModule
} from "./chunk-KE3QZEOK.js";
import {
  NgModule,
  setClassMetadata,
  ɵɵdefineInjector,
  ɵɵdefineNgModule
} from "./chunk-BX3JX6U3.js";

// node_modules/@angular/material/fesm2022/pseudo-checkbox-module-CAX2sutq.mjs
var MatPseudoCheckboxModule = class _MatPseudoCheckboxModule {
  static ɵfac = function MatPseudoCheckboxModule_Factory(__ngFactoryType__) {
    return new (__ngFactoryType__ || _MatPseudoCheckboxModule)();
  };
  static ɵmod = ɵɵdefineNgModule({
    type: _MatPseudoCheckboxModule,
    imports: [MatCommonModule, MatPseudoCheckbox],
    exports: [MatPseudoCheckbox]
  });
  static ɵinj = ɵɵdefineInjector({
    imports: [MatCommonModule]
  });
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(MatPseudoCheckboxModule, [{
    type: NgModule,
    args: [{
      imports: [MatCommonModule, MatPseudoCheckbox],
      exports: [MatPseudoCheckbox]
    }]
  }], null, null);
})();

export {
  MatPseudoCheckboxModule
};
//# sourceMappingURL=chunk-YSAW5ZGB.js.map
