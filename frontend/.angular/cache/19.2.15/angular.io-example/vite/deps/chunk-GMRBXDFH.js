// node_modules/@angular/cdk/fesm2022/portal.mjs
var PortalInjector = class {
  _parentInjector;
  _customTokens;
  constructor(_parentInjector, _customTokens) {
    this._parentInjector = _parentInjector;
    this._customTokens = _customTokens;
  }
  get(token, notFoundValue) {
    const value = this._customTokens.get(token);
    if (typeof value !== "undefined") {
      return value;
    }
    return this._parentInjector.get(token, notFoundValue);
  }
};

export {
  PortalInjector
};
//# sourceMappingURL=chunk-GMRBXDFH.js.map
