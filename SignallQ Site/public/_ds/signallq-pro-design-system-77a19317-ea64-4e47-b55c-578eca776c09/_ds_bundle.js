/* @ds-bundle: {"format":4,"namespace":"SignallQPRODesignSystem_77a193","components":[{"name":"Button","sourcePath":"components/Button/Button.jsx"},{"name":"ComparisonBlock","sourcePath":"components/ComparisonBlock/ComparisonBlock.jsx"},{"name":"EnvironmentCard","sourcePath":"components/EnvironmentCard/EnvironmentCard.jsx"},{"name":"EvidenceChip","sourcePath":"components/EvidenceChip/EvidenceChip.jsx"},{"name":"ListRow","sourcePath":"components/ListRow/ListRow.jsx"},{"name":"Navbar","sourcePath":"components/Navbar/Navbar.jsx"},{"name":"NAV_ITEMS","sourcePath":"components/Navbar/Navbar.jsx"},{"name":"QualityGauge","sourcePath":"components/QualityGauge/QualityGauge.jsx"},{"name":"RecommendationBlock","sourcePath":"components/RecommendationBlock/RecommendationBlock.jsx"},{"name":"SignatureBlock","sourcePath":"components/SignatureBlock/SignatureBlock.jsx"},{"name":"StateCard","sourcePath":"components/StateCard/StateCard.jsx"},{"name":"StatusChip","sourcePath":"components/StatusChip/StatusChip.jsx"},{"name":"ATENDIMENTO_STATUS","sourcePath":"components/StatusChip/StatusChip.jsx"},{"name":"VISITA_STATUS","sourcePath":"components/StatusChip/StatusChip.jsx"},{"name":"MEDICAO_STATUS","sourcePath":"components/StatusChip/StatusChip.jsx"},{"name":"SyncBanner","sourcePath":"components/SyncBanner/SyncBanner.jsx"},{"name":"TextField","sourcePath":"components/TextField/TextField.jsx"},{"name":"TopBar","sourcePath":"components/TopBar/TopBar.jsx"},{"name":"VisitCard","sourcePath":"components/VisitCard/VisitCard.jsx"}],"sourceHashes":{"components/Button/Button.jsx":"a83e2ff0fe1e","components/ComparisonBlock/ComparisonBlock.jsx":"d09720b02e97","components/EnvironmentCard/EnvironmentCard.jsx":"d8535002800e","components/EvidenceChip/EvidenceChip.jsx":"954836f9b2a2","components/ListRow/ListRow.jsx":"6a2503b65036","components/Navbar/Navbar.jsx":"81bfb6ed431e","components/QualityGauge/QualityGauge.jsx":"95b302d218a5","components/RecommendationBlock/RecommendationBlock.jsx":"8ef5bf5e41ea","components/SignatureBlock/SignatureBlock.jsx":"0a0ed72ff778","components/StateCard/StateCard.jsx":"3fb3f0703bdd","components/StatusChip/StatusChip.jsx":"c038e7852fec","components/SyncBanner/SyncBanner.jsx":"37682990161c","components/TextField/TextField.jsx":"4a82a520fe48","components/TopBar/TopBar.jsx":"65309850193d","components/VisitCard/VisitCard.jsx":"f8a62597ce70"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.SignallQPRODesignSystem_77a193 = window.SignallQPRODesignSystem_77a193 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/Button/Button.jsx
try { (() => {
function Button({
  variant = 'primary',
  children,
  disabled = false,
  icon,
  onClick,
  type = 'button'
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: type,
    className: `sqp-btn sqp-btn--${variant}`,
    disabled: disabled,
    onClick: onClick
  }, icon, children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/Button/Button.jsx", error: String((e && e.message) || e) }); }

// components/ComparisonBlock/ComparisonBlock.jsx
try { (() => {
function ComparisonBlock({
  label,
  before,
  after,
  unit,
  higherIsBetter = true,
  conclusion
}) {
  const delta = after - before;
  const percent = before !== 0 ? delta / Math.abs(before) * 100 : 0;
  const improved = higherIsBetter ? delta > 0 : delta < 0;
  const arrow = delta === 0 ? '→' : delta > 0 ? '↑' : '↓';
  const tone = delta === 0 ? '' : improved ? 'positive' : 'critical';
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-compare"
  }, /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, label), /*#__PURE__*/React.createElement("div", {
    className: "sqp-compare__values"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-compare__value"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-type-label"
  }, "ANTES"), /*#__PURE__*/React.createElement("span", {
    className: "sqp-compare__value-num"
  }, before, unit)), /*#__PURE__*/React.createElement("span", {
    className: `sqp-compare__arrow ${tone ? `sqp-compare__arrow--${tone}` : ''}`
  }, arrow), /*#__PURE__*/React.createElement("div", {
    className: "sqp-compare__value"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-type-label"
  }, "DEPOIS"), /*#__PURE__*/React.createElement("span", {
    className: "sqp-compare__value-num"
  }, after, unit))), /*#__PURE__*/React.createElement("p", {
    className: `sqp-compare__delta ${tone ? `sqp-compare__arrow--${tone}` : ''}`
  }, delta > 0 ? '+' : '', delta, unit, " (", percent > 0 ? '+' : '', percent.toFixed(0), "%)"), conclusion && /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-body"
  }, conclusion));
}
Object.assign(__ds_scope, { ComparisonBlock });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/ComparisonBlock/ComparisonBlock.jsx", error: String((e && e.message) || e) }); }

// components/EvidenceChip/EvidenceChip.jsx
try { (() => {
const KIND_LABEL = {
  foto: 'Foto',
  observacao: 'Observação',
  equipamento: 'Equipamento',
  configuracao: 'Configuração'
};
function EvidenceChip({
  kind = 'foto',
  label
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: "sqp-evidence"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-evidence__icon"
  }), /*#__PURE__*/React.createElement("span", {
    className: "sqp-type-support"
  }, /*#__PURE__*/React.createElement("strong", null, KIND_LABEL[kind] ?? kind), " \xB7 ", label));
}
Object.assign(__ds_scope, { EvidenceChip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/EvidenceChip/EvidenceChip.jsx", error: String((e && e.message) || e) }); }

// components/ListRow/ListRow.jsx
try { (() => {
function Chevron() {
  return /*#__PURE__*/React.createElement("svg", {
    className: "sqp-listrow__chevron",
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M9 6l6 6-6 6"
  }));
}
function ListRow({
  leading,
  title,
  subtitle,
  trailing,
  onClick,
  chevron = true
}) {
  const clickable = !!onClick;
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-listrow",
    role: clickable ? 'button' : undefined,
    tabIndex: clickable ? 0 : undefined,
    onClick: onClick
  }, leading ? /*#__PURE__*/React.createElement("span", {
    className: "sqp-listrow__leading"
  }, leading) : null, /*#__PURE__*/React.createElement("div", {
    className: "sqp-listrow__body"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-listrow__title"
  }, title), subtitle ? /*#__PURE__*/React.createElement("span", {
    className: "sqp-listrow__subtitle"
  }, subtitle) : null), trailing ? /*#__PURE__*/React.createElement("span", {
    className: "sqp-listrow__trailing"
  }, trailing) : null, clickable && chevron ? /*#__PURE__*/React.createElement(Chevron, null) : null);
}
Object.assign(__ds_scope, { ListRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/ListRow/ListRow.jsx", error: String((e && e.message) || e) }); }

// components/Navbar/Navbar.jsx
try { (() => {
const NAV_ITEMS = [{
  key: 'inicio',
  label: 'Início',
  icon: 'home'
}, {
  key: 'atendimentos',
  label: 'Atendimentos',
  icon: 'clipboard'
}, {
  key: 'clientes',
  label: 'Clientes',
  icon: 'people'
}, {
  key: 'ajustes',
  label: 'Ajustes',
  icon: 'settings'
}];
function NavIcon({
  name
}) {
  const p = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    className: 'sqp-icon sqp-navbar__icon'
  };
  if (name === 'home') return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("path", {
    d: "M3 11l9-7 9 7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 10v9a1 1 0 001 1h4v-6h4v6h4a1 1 0 001-1v-9"
  }));
  if (name === 'clipboard') return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("rect", {
    x: "6",
    y: "4",
    width: "12",
    height: "17",
    rx: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 4V3a1 1 0 011-1h4a1 1 0 011 1v1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 11h6M9 15h6"
  }));
  if (name === 'people') return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("circle", {
    cx: "9",
    cy: "8",
    r: "3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 20c0-3 2.5-5 6-5s6 2 6 5"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "17",
    cy: "9",
    r: "2.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M15.5 15.2c2.3.4 3.5 2 3.5 4.8"
  }));
  if (name === 'settings') return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 3v2.2M12 18.8V21M4.9 4.9l1.5 1.5M17.6 17.6l1.5 1.5M3 12h2.2M18.8 12H21M4.9 19.1l1.5-1.5M17.6 6.4l1.5-1.5"
  }));
  return null;
}
function Navbar({
  items = NAV_ITEMS,
  active,
  onSelect,
  position = 'bottom'
}) {
  return /*#__PURE__*/React.createElement("nav", {
    className: `sqp-navbar${position === 'rail' ? ' sqp-navbar--rail' : ''}`
  }, items.map(it => /*#__PURE__*/React.createElement("button", {
    key: it.key,
    type: "button",
    className: "sqp-navbar__item",
    "aria-current": it.key === active,
    onClick: () => onSelect?.(it.key)
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-navbar__indicator"
  }, /*#__PURE__*/React.createElement(NavIcon, {
    name: it.icon
  })), /*#__PURE__*/React.createElement("span", {
    className: "sqp-navbar__label"
  }, it.label))));
}
Object.assign(__ds_scope, { Navbar, NAV_ITEMS });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/Navbar/Navbar.jsx", error: String((e && e.message) || e) }); }

// components/QualityGauge/QualityGauge.jsx
try { (() => {
function QualityGauge({
  label,
  value,
  unit,
  min = 0,
  max = 100,
  status = 'nao_avaliado',
  context
}) {
  const toneFill = {
    bom: 'positive',
    atencao: 'attention',
    critico: 'critical',
    nao_avaliado: 'neutral'
  }[status] ?? 'neutral';
  const pct = Math.min(100, Math.max(0, (value - min) / (max - min) * 100));
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-gauge"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-gauge__row"
  }, /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, label)), /*#__PURE__*/React.createElement("p", null, /*#__PURE__*/React.createElement("span", {
    className: "sqp-gauge__value"
  }, value), /*#__PURE__*/React.createElement("span", {
    className: "sqp-gauge__unit"
  }, unit)), /*#__PURE__*/React.createElement("div", {
    className: "sqp-gauge__track"
  }, /*#__PURE__*/React.createElement("div", {
    className: `sqp-gauge__fill sqp-gauge__fill--${toneFill}`,
    style: {
      width: `${pct}%`
    }
  })), context && /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, context));
}
Object.assign(__ds_scope, { QualityGauge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/QualityGauge/QualityGauge.jsx", error: String((e && e.message) || e) }); }

// components/RecommendationBlock/RecommendationBlock.jsx
try { (() => {
const PRIORITY_LABEL = {
  alta: 'Prioridade alta',
  media: 'Prioridade média',
  baixa: 'Prioridade baixa'
};
function RecommendationBlock({
  problem,
  impact,
  action,
  priority = 'media'
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: `sqp-recommendation sqp-recommendation--${priority}`
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-recommendation__priority",
    style: {
      color: `var(--sqp-color-${priority === 'alta' ? 'critical' : priority === 'baixa' ? 'primary' : 'attention'})`
    }
  }, PRIORITY_LABEL[priority] ?? priority), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-body"
  }, /*#__PURE__*/React.createElement("strong", null, problem)), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, impact), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-body"
  }, action));
}
Object.assign(__ds_scope, { RecommendationBlock });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/RecommendationBlock/RecommendationBlock.jsx", error: String((e && e.message) || e) }); }

// components/SignatureBlock/SignatureBlock.jsx
try { (() => {
function SignatureBlock({
  name,
  date,
  confirmed = false,
  note
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-signature"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-signature__row"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-body"
  }, /*#__PURE__*/React.createElement("strong", null, name)), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, date)), confirmed && /*#__PURE__*/React.createElement("span", {
    className: "sqp-signature__confirmed"
  }, "Aceite confirmado")), note && /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, note));
}
Object.assign(__ds_scope, { SignatureBlock });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/SignatureBlock/SignatureBlock.jsx", error: String((e && e.message) || e) }); }

// components/StateCard/StateCard.jsx
try { (() => {
function StateIcon({
  variant
}) {
  const p = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    className: 'sqp-icon'
  };
  if (variant === 'erro') return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("path", {
    d: "M12 9v4M12 17h.01"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10.3 3.9L2.7 17.1A1.7 1.7 0 004.2 19.6h15.6a1.7 1.7 0 001.5-2.5L13.7 3.9a1.7 1.7 0 00-3.4 0z"
  }));
  if (variant === 'concluido') return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("path", {
    d: "M20 6L9 17l-5-5"
  }));
  return /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("path", {
    d: "M3 7l2-3h14l2 3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 7h18v12a2 2 0 01-2 2H5a2 2 0 01-2-2V7z"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 11h6"
  }));
}
function StateCard({
  variant = 'vazio',
  title,
  description,
  actionLabel,
  onAction
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: `sqp-statecard sqp-statecard--${variant}`
  }, variant === 'carregando' ? /*#__PURE__*/React.createElement("div", {
    className: "sqp-statecard__skeleton"
  }, /*#__PURE__*/React.createElement("span", null), /*#__PURE__*/React.createElement("span", null), /*#__PURE__*/React.createElement("span", null)) : /*#__PURE__*/React.createElement("span", {
    className: "sqp-statecard__icon"
  }, /*#__PURE__*/React.createElement(StateIcon, {
    variant: variant
  })), title ? /*#__PURE__*/React.createElement("p", {
    className: "sqp-statecard__title"
  }, title) : null, description ? /*#__PURE__*/React.createElement("p", {
    className: "sqp-statecard__desc"
  }, description) : null, actionLabel ? /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "sqp-btn sqp-btn--secondary",
    onClick: onAction
  }, actionLabel) : null);
}
Object.assign(__ds_scope, { StateCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/StateCard/StateCard.jsx", error: String((e && e.message) || e) }); }

// components/StatusChip/StatusChip.jsx
try { (() => {
function StatusChip({
  tone = 'neutral',
  label,
  children
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: `sqp-chip sqp-chip--${tone}`
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-chip__dot"
  }), label ?? children);
}
const ATENDIMENTO_STATUS = {
  solicitado: {
    tone: 'neutral',
    label: 'Solicitado'
  },
  confirmado: {
    tone: 'primary',
    label: 'Confirmado'
  },
  a_caminho: {
    tone: 'accent',
    label: 'A caminho'
  },
  em_atendimento: {
    tone: 'attention',
    label: 'Em atendimento'
  },
  concluido: {
    tone: 'positive',
    label: 'Concluído'
  },
  cancelado: {
    tone: 'critical',
    label: 'Cancelado / não compareceu'
  }
};
const VISITA_STATUS = {
  rascunho: {
    tone: 'neutral',
    label: 'Rascunho'
  },
  em_andamento: {
    tone: 'primary',
    label: 'Em andamento'
  },
  aguardando_validacao: {
    tone: 'attention',
    label: 'Aguardando validação'
  },
  concluida: {
    tone: 'positive',
    label: 'Concluída'
  },
  cancelada: {
    tone: 'critical',
    label: 'Cancelada'
  }
};
const MEDICAO_STATUS = {
  bom: {
    tone: 'positive',
    label: 'Bom'
  },
  atencao: {
    tone: 'attention',
    label: 'Atenção'
  },
  critico: {
    tone: 'critical',
    label: 'Crítico'
  },
  nao_avaliado: {
    tone: 'neutral',
    label: 'Não avaliado'
  }
};
Object.assign(__ds_scope, { StatusChip, ATENDIMENTO_STATUS, VISITA_STATUS, MEDICAO_STATUS });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/StatusChip/StatusChip.jsx", error: String((e && e.message) || e) }); }

// components/EnvironmentCard/EnvironmentCard.jsx
try { (() => {
function EnvironmentCard({
  name,
  type,
  progress = 0,
  metrics = [],
  status = 'nao_avaliado',
  onOpen
}) {
  const preset = __ds_scope.MEDICAO_STATUS[status] ?? __ds_scope.MEDICAO_STATUS.nao_avaliado;
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-card sqp-envcard",
    onClick: onOpen,
    role: onOpen ? 'button' : undefined
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-envcard__head"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-section"
  }, name), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, type)), /*#__PURE__*/React.createElement(__ds_scope.StatusChip, {
    tone: preset.tone,
    label: preset.label
  })), /*#__PURE__*/React.createElement("div", {
    className: "sqp-envcard__progress-track"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-envcard__progress-fill",
    style: {
      width: `${Math.min(100, Math.max(0, progress))}%`
    }
  })), /*#__PURE__*/React.createElement("div", {
    className: "sqp-envcard__metrics"
  }, metrics.map((m, i) => /*#__PURE__*/React.createElement("span", {
    className: "sqp-envcard__metric",
    key: i
  }, m))));
}
Object.assign(__ds_scope, { EnvironmentCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/EnvironmentCard/EnvironmentCard.jsx", error: String((e && e.message) || e) }); }

// components/SyncBanner/SyncBanner.jsx
try { (() => {
const COPY = {
  offline: {
    text: 'Sem conexão — os dados estão salvos neste aparelho.'
  },
  pendente: {
    text: n => `${n} ${n === 1 ? 'item' : 'itens'} pendente${n === 1 ? '' : 's'} de sincronização.`
  },
  sincronizado: {
    text: t => `Sincronizado. Última sincronização: ${t}.`
  }
};
function SyncBanner({
  state = 'pendente',
  pendingCount = 0,
  lastSyncLabel
}) {
  const label = state === 'pendente' ? COPY.pendente.text(pendingCount) : state === 'sincronizado' ? COPY.sincronizado.text(lastSyncLabel ?? 'agora') : COPY.offline.text;
  return /*#__PURE__*/React.createElement("div", {
    className: `sqp-syncbanner sqp-syncbanner--${state}`
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-syncbanner__left"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-syncbanner__dot"
  }), /*#__PURE__*/React.createElement("span", {
    className: "sqp-type-support",
    style: {
      color: 'inherit'
    }
  }, label)));
}
Object.assign(__ds_scope, { SyncBanner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/SyncBanner/SyncBanner.jsx", error: String((e && e.message) || e) }); }

// components/TextField/TextField.jsx
try { (() => {
function TextField({
  label,
  placeholder,
  value,
  onChange,
  helperText,
  error = false,
  disabled = false,
  type = 'text',
  id
}) {
  const fieldId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);
  const cls = `sqp-field${error ? ' sqp-field--error' : ''}${disabled ? ' sqp-field--disabled' : ''}`;
  return /*#__PURE__*/React.createElement("div", {
    className: cls
  }, label ? /*#__PURE__*/React.createElement("label", {
    className: "sqp-field__label",
    htmlFor: fieldId
  }, label) : null, /*#__PURE__*/React.createElement("div", {
    className: "sqp-field__control"
  }, /*#__PURE__*/React.createElement("input", {
    id: fieldId,
    className: "sqp-field__input",
    type: type,
    placeholder: placeholder,
    value: value,
    onChange: onChange,
    disabled: disabled
  })), helperText ? /*#__PURE__*/React.createElement("span", {
    className: "sqp-field__helper"
  }, helperText) : null);
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/TextField/TextField.jsx", error: String((e && e.message) || e) }); }

// components/TopBar/TopBar.jsx
try { (() => {
function LeadingIcon({
  kind
}) {
  const p = {
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round',
    strokeLinejoin: 'round',
    className: 'sqp-icon'
  };
  return kind === 'close' ? /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("path", {
    d: "M18 6L6 18M6 6l12 12"
  })) : /*#__PURE__*/React.createElement("svg", p, /*#__PURE__*/React.createElement("path", {
    d: "M15 18l-6-6 6-6"
  }));
}
const SAVE_DOT_COLOR = {
  salvo: 'var(--md-sys-color-success)',
  salvando: 'var(--md-sys-color-on-surface-variant)',
  offline: 'var(--md-sys-color-warning)'
};
function TopBar({
  title,
  subtitle,
  leading = 'back',
  onLeadingClick,
  saveState,
  action,
  onAction
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-topbar"
  }, leading ? /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "sqp-topbar__icon-btn",
    "aria-label": leading === 'close' ? 'Fechar' : 'Voltar',
    onClick: onLeadingClick
  }, /*#__PURE__*/React.createElement(LeadingIcon, {
    kind: leading
  })) : null, /*#__PURE__*/React.createElement("div", {
    className: "sqp-topbar__title-wrap"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-topbar__title"
  }, title), subtitle ? /*#__PURE__*/React.createElement("span", {
    className: "sqp-topbar__subtitle"
  }, saveState ? /*#__PURE__*/React.createElement("span", {
    className: "sqp-topbar__save-dot",
    style: {
      background: SAVE_DOT_COLOR[saveState]
    }
  }) : null, subtitle) : null), action ? /*#__PURE__*/React.createElement("div", {
    className: "sqp-topbar__actions"
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "sqp-btn sqp-btn--text",
    onClick: onAction
  }, action)) : null);
}
Object.assign(__ds_scope, { TopBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/TopBar/TopBar.jsx", error: String((e && e.message) || e) }); }

// components/VisitCard/VisitCard.jsx
try { (() => {
function VisitCard({
  clientName,
  locationName,
  objective,
  status = 'em_andamento',
  time,
  continueLabel = 'Continuar',
  onContinue
}) {
  const preset = __ds_scope.VISITA_STATUS[status] ?? __ds_scope.VISITA_STATUS.em_andamento;
  return /*#__PURE__*/React.createElement("div", {
    className: "sqp-card sqp-visitcard"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-visitcard__top"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sqp-visitcard__client"
  }, /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-section"
  }, clientName), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-support"
  }, locationName)), /*#__PURE__*/React.createElement(__ds_scope.StatusChip, {
    tone: preset.tone,
    label: preset.label
  })), /*#__PURE__*/React.createElement("p", {
    className: "sqp-type-body"
  }, objective), /*#__PURE__*/React.createElement("div", {
    className: "sqp-visitcard__footer"
  }, /*#__PURE__*/React.createElement("span", {
    className: "sqp-type-support"
  }, time), /*#__PURE__*/React.createElement("button", {
    className: "sqp-btn sqp-btn--text",
    onClick: onContinue
  }, continueLabel)));
}
Object.assign(__ds_scope, { VisitCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/VisitCard/VisitCard.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.ComparisonBlock = __ds_scope.ComparisonBlock;

__ds_ns.EnvironmentCard = __ds_scope.EnvironmentCard;

__ds_ns.EvidenceChip = __ds_scope.EvidenceChip;

__ds_ns.ListRow = __ds_scope.ListRow;

__ds_ns.Navbar = __ds_scope.Navbar;

__ds_ns.NAV_ITEMS = __ds_scope.NAV_ITEMS;

__ds_ns.QualityGauge = __ds_scope.QualityGauge;

__ds_ns.RecommendationBlock = __ds_scope.RecommendationBlock;

__ds_ns.SignatureBlock = __ds_scope.SignatureBlock;

__ds_ns.StateCard = __ds_scope.StateCard;

__ds_ns.StatusChip = __ds_scope.StatusChip;

__ds_ns.ATENDIMENTO_STATUS = __ds_scope.ATENDIMENTO_STATUS;

__ds_ns.VISITA_STATUS = __ds_scope.VISITA_STATUS;

__ds_ns.MEDICAO_STATUS = __ds_scope.MEDICAO_STATUS;

__ds_ns.SyncBanner = __ds_scope.SyncBanner;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.TopBar = __ds_scope.TopBar;

__ds_ns.VisitCard = __ds_scope.VisitCard;

})();
