#!/usr/bin/env python3
"""Run the opt-in Android/web persistence test and reject zero/omitted tests."""
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

required = ('EMAIL', 'PASSWORD', 'TITLE', 'COUNT', 'SETS')
missing = [key for key in required if not os.environ.get('ACADEMIA_E2E_' + key)]
if missing:
    sys.exit('Defina ACADEMIA_E2E_' + ', ACADEMIA_E2E_'.join(missing))
root = Path(__file__).resolve().parent.parent
result = subprocess.run([
    'sh', str(root / 'gradlew'), 'connectedDebugAndroidTest',
    '-Pandroid.testInstrumentationRunnerArguments.class=com.example.app_academiafr.PublishedWorkoutTest',
    '--console=plain', '--no-configuration-cache'
], cwd=root)
if result.returncode:
    sys.exit(result.returncode)
cases = []
for report in (root / 'app/build/outputs/androidTest-results/connected/debug').glob('TEST-*.xml'):
    cases.extend(ET.parse(report).iter('testcase'))
if not cases or any(case.find('failure') is not None or case.find('error') is not None or case.find('skipped') is not None for case in cases):
    sys.exit('FALHOU: integração não executada integralmente; confira os relatórios Android.')
print(f'OK: {len(cases)} teste(s) de persistência realmente executado(s), sem falhas ou omissões.')
