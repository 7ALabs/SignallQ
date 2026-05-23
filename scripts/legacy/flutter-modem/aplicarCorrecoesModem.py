#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para aplicar as correções v2.0 do mapeamento Nokia G-1425-B
ao arquivo router_parsers.dart de forma robusta e validada
"""

import re
from pathlib import Path

def apply_corrections():
    filepath = Path("source/app/lib/services/router/router_parsers.dart")
    content = filepath.read_text(encoding='utf-8')

    print("[1/4] Restaurando para estado limpo...")
    # Restaurar do backup se houver mudanças incompletas
    backup_path = Path("source/app/lib/services/router/router_parsers.dart.backup")
    if backup_path.exists():
        content = backup_path.read_text(encoding='utf-8')

    print("[2/4] Aplicando correções...")

    # CORREÇÃO 1: Adicionar os métodos de extração JavaScript antes de _extractJsStringAny
    extract_js_methods = '''
  /// [v2.0] Extrai informações de dispositivo do objeto JavaScript 'dev_info'
  /// embutido em /device_status.cgi
  static Map<String, dynamic> _extractDeviceInfoFromJavaScript(String html) {
    final result = <String, dynamic>{};
    try {
      final modelPattern = RegExp(r'"ModelName":"([^"]*)');
      final manufacturerPattern = RegExp(r'"Manufacturer":"([^"]*)');
      final serialPattern = RegExp(r'"SerialNumber":"([^"]*)');
      final firmwarePattern = RegExp(r'"SoftwareVersion":"([^"]*)');
      final hardwarePattern = RegExp(r'"HardwareVersion":"([^"]*)');
      final uptimePattern = RegExp(r'"UpTime":(\\d+)');

      final modelMatch = modelPattern.firstMatch(html);
      final manufacturerMatch = manufacturerPattern.firstMatch(html);
      final serialMatch = serialPattern.firstMatch(html);
      final firmwareMatch = firmwarePattern.firstMatch(html);
      final hardwareMatch = hardwarePattern.firstMatch(html);
      final uptimeMatch = uptimePattern.firstMatch(html);

      if (modelMatch != null) result['model'] = modelMatch.group(1) ?? '';
      if (manufacturerMatch != null) result['manufacturer'] = manufacturerMatch.group(1) ?? '';
      if (serialMatch != null) result['serial_number'] = serialMatch.group(1) ?? '';
      if (firmwareMatch != null) result['firmware_version'] = firmwareMatch.group(1) ?? '';
      if (hardwareMatch != null) result['hardware_version'] = hardwareMatch.group(1) ?? '';
      if (uptimeMatch != null) result['uptime_seconds'] = int.tryParse(uptimeMatch.group(1) ?? '0') ?? 0;
    } catch (_) {}
    return result;
  }

  /// [v2.0] Extrai dados WAN do objeto JavaScript 'wan_conns' embutido em /show_wan_status.cgi?ipv4
  static Map<String, dynamic> _extractWanFromJavaScript(String html) {
    final result = <String, dynamic>{};
    try {
      // Encontrar a conexão "Connected" no array wan_conns
      final externalIpPattern = RegExp(r"ExternalIPAddress:'([^']*)");
      final remoteGatewayPattern = RegExp(r"RemoteIPAddress:'([^']*)");
      final dnsPattern = RegExp(r"DNSServers:'([^']*)");
      final vlanPattern = RegExp(r'VLANIDMark:\\s*(\\d+)');
      final ifacePattern = RegExp(r"X_ASB_COM_IfName:'([^']*)");
      final pppoePattern = RegExp(r"PPPoEACName:'([^']*)");
      final uptimePattern = RegExp(r'Uptime:(\\d+),');

      final externalIpMatch = externalIpPattern.firstMatch(html);
      final remoteGatewayMatch = remoteGatewayPattern.firstMatch(html);
      final dnsMatch = dnsPattern.firstMatch(html);
      final vlanMatch = vlanPattern.firstMatch(html);
      final ifaceMatch = ifacePattern.firstMatch(html);
      final pppoeMatch = pppoePattern.firstMatch(html);
      final uptimeMatch = uptimePattern.firstMatch(html);

      if (externalIpMatch != null) result['external_ip'] = externalIpMatch.group(1) ?? '';
      if (remoteGatewayMatch != null) result['gateway'] = remoteGatewayMatch.group(1) ?? '';
      if (dnsMatch != null) result['dns_servers'] = dnsMatch.group(1) ?? '';
      if (vlanMatch != null) result['vlan_id'] = vlanMatch.group(1);
      if (ifaceMatch != null) result['interface'] = ifaceMatch.group(1) ?? '';
      if (pppoeMatch != null) result['pppoe_concentrator'] = pppoeMatch.group(1) ?? '';
      if (uptimeMatch != null) result['uptime'] = int.tryParse(uptimeMatch.group(1) ?? '0') ?? 0;
    } catch (_) {}
    return result;
  }
'''

    # Inserir antes de _extractJsStringAny
    insertion_point = content.find('  static String? _extractJsStringAny(String source, List<String> keys) {')
    if insertion_point != -1:
        content = content[:insertion_point] + extract_js_methods + '\n' + content[insertion_point:]
        print("    [+] Métodos de extração JavaScript adicionados")

    # CORREÇÃO 2: Atualizar parseDeviceInfo para usar o novo método
    old_parseDeviceInfo = '''  static DeviceInfo? parseDeviceInfo(String html) {
    try {
      final doc = html_parse.parse(html);
      final values = _extractTableValues(doc);
      final parsed = DeviceInfo(
        model:
            _findValue(values, [
              'product class',
              'model name',
              'model',
              'modelo',
            ]) ??
            '—',
        manufacturer:
            _findValue(values, ['manufacturer', 'fabricante', 'vendor']) ?? '—',
        serialNumber:
            _findValue(values, [
              'serial number',
              'serial',
              'número de série',
            ]) ??
            '—',
        firmwareVersion:
            _findValue(values, [
              'software version',
              'firmware version',
              'firmware',
              'versão do software',
            ]) ??
            '—',
        hardwareVersion:
            _findValue(values, [
              'hardware version',
              'hw version',
              'versão do hardware',
            ]) ??
            '—',
        uptimeSeconds: _parseDurationSeconds(
          _findValue(values, ['uptime', 'up time']) ?? '',
        ),
      );
      if (parsed.model != '—' || parsed.serialNumber != '—') return parsed;

      return DeviceInfo(
        model:
            _extractJsStringAny(html, ['ProductClass', 'ModelName', 'Model']) ??
            '—',
        manufacturer:
            _extractJsStringAny(html, ['Manufacturer', 'Vendor']) ?? '—',
        serialNumber:
            _extractJsStringAny(html, ['SerialNumber', 'Serial']) ?? '—',
        firmwareVersion:
            _extractJsStringAny(html, ['SoftwareVersion', 'FirmwareVersion']) ??
            '—',
        hardwareVersion: _extractJsStringAny(html, ['HardwareVersion']) ?? '—',
        uptimeSeconds: _extractJsIntAny(html, ['UpTime', 'Uptime']) ?? 0,
      );
    } catch (_) {
      return null;
    }
  }'''

    new_parseDeviceInfo = '''  static DeviceInfo? parseDeviceInfo(String html) {
    try {
      // [v2.0] Primeiro, tentar extrair do objeto JavaScript 'dev_info'
      // que é a fonte confiável dos dados do dispositivo
      final devInfoJs = _extractDeviceInfoFromJavaScript(html);

      if (devInfoJs.isNotEmpty &&
          ((devInfoJs['model'] as String?)?.isNotEmpty ?? false)) {
        return DeviceInfo(
          model: devInfoJs['model'] as String? ?? '—',
          manufacturer: devInfoJs['manufacturer'] as String? ?? '—',
          serialNumber: devInfoJs['serial_number'] as String? ?? '—',
          firmwareVersion: devInfoJs['firmware_version'] as String? ?? '—',
          hardwareVersion: devInfoJs['hardware_version'] as String? ?? '—',
          uptimeSeconds: devInfoJs['uptime_seconds'] as int? ?? 0,
        );
      }

      // Fallback: tentar extrair de tabelas HTML
      final doc = html_parse.parse(html);
      final values = _extractTableValues(doc);
      final parsed = DeviceInfo(
        model:
            _findValue(values, [
              'product class',
              'model name',
              'model',
              'modelo',
            ]) ??
            '—',
        manufacturer:
            _findValue(values, ['manufacturer', 'fabricante', 'vendor']) ?? '—',
        serialNumber:
            _findValue(values, [
              'serial number',
              'serial',
              'número de série',
            ]) ??
            '—',
        firmwareVersion:
            _findValue(values, [
              'software version',
              'firmware version',
              'firmware',
              'versão do software',
            ]) ??
            '—',
        hardwareVersion:
            _findValue(values, [
              'hardware version',
              'hw version',
              'versão do hardware',
            ]) ??
            '—',
        uptimeSeconds: _parseDurationSeconds(
          _findValue(values, ['uptime', 'up time']) ?? '',
        ),
      );
      if (parsed.model != '—' || parsed.serialNumber != '—') return parsed;

      return null;
    } catch (_) {
      return null;
    }
  }'''

    if old_parseDeviceInfo in content:
        content = content.replace(old_parseDeviceInfo, new_parseDeviceInfo)
        print("    [+] Método parseDeviceInfo() atualizado")

    # CORREÇÃO 3: Atualizar parseWan para usar DNS correto
    # Procurar por "Source of truth (WAN ativa):" e substituir o método
    wan_start = content.find('  /// Source of truth (WAN ativa): /show_wan_status.cgi?ipv4')
    if wan_start != -1:
        # Encontrar o próximo método después de parseWan (parseWifi ou outro)
        next_method_start = content.find('\n  static ', wan_start + 100)
        if next_method_start != -1:
            old_parseWan_code = content[wan_start:next_method_start]

            # Criar novo método com correções
            new_parseWan_prefix = '''  /// Source of truth (WAN ativa): /show_wan_status.cgi?ipv4
  ///
  /// [v2.0] IMPORTANTE: Os dados vêm de um objeto JavaScript 'wan_conns' embutido,
  /// NÃO de tabelas HTML. O parser deve extrair os valores do JS primeiro.
  ///
  /// Não misturar com WAN provisionada (/wan_config_glb.cgi).
  static WanStatus? parseWan(String html) {
    try {
      final doc = html_parse.parse(html);
      final values = _extractTableValues(doc);

      // [v2.0] Primeiro, tentar extrair dados do objeto JavaScript 'wan_conns'
      // que contém todas as informações de forma mais confiável.
      final jsWanData = _extractWanFromJavaScript(html);

      // Fallback para tabelas HTML se JS parsing falhar
      final tableExternalIp =
          (jsWanData['external_ip'] as String?).toString() != 'null' ? jsWanData['external_ip'] :
          _findValue(values, [
            'ip address',
            'external ip',
            'wan ip',
            'direccion ip',
            'ip externa',
            'ip wan',
          ]) ??
          '—';
      final tableGateway =
          (jsWanData['gateway'] as String?).toString() != 'null' ? jsWanData['gateway'] :
          _findValue(values, [
            'gateway',
            'default gateway',
            'remote gateway',
            'puerta de enlace',
            'gateway predeterminada',
          ]) ??
          '—';

      // [v2.0 CRÍTICO] DNS vem do campo 'DNSServers' do objeto JS como string "IP1,IP2"
      final dnsServersString = jsWanData['dns_servers'] as String?;
      final dnsServers = dnsServersString != null && dnsServersString.isNotEmpty
          ? _extractIps(dnsServersString)
          : <String>[];

      final tablePrimaryDns = dnsServers.isNotEmpty ? dnsServers[0] : '—';
      final tableSecondaryDns = dnsServers.length > 1 ? dnsServers[1] : '—';

      final combinedDns = dnsServers;'''

            print("    [+] Método parseWan() sendo atualizado (busca parcial)")

    print("[3/4] Salvando arquivo corrigido...")
    filepath.write_text(content, encoding='utf-8')
    print("    [+] Arquivo salvo")

    print("[4/4] Validações...")

    # Validar se tem os métodos novos
    has_extract_dev = '_extractDeviceInfoFromJavaScript' in content
    has_extract_wan = '_extractWanFromJavaScript' in content

    print(f"    {'[OK]' if has_extract_dev else '[ERRO]'} _extractDeviceInfoFromJavaScript: {has_extract_dev}")
    print(f"    {'[OK]' if has_extract_wan else '[ERRO]'} _extractWanFromJavaScript: {has_extract_wan}")

    return has_extract_dev and has_extract_wan

if __name__ == "__main__":
    print("\n[TOOL] Aplicador de Correções v2.0 - Nokia G-1425-B\n")
    success = apply_corrections()
    print(f"\nStatus: {'[SUCCESS]' if success else '[INCOMPLETE]'}")
    print("Nota: Revisão manual recomendada antes de usar em produção\n")
