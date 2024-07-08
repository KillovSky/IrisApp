package com.killovsky.iris

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ConfigurationActivity : AppCompatActivity() {

    private lateinit var listaConfiguracoes: ListView
    private lateinit var botaoAdicionarConfiguracao: Button
    private val configuracoes = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        listaConfiguracoes = findViewById(R.id.configListView)
        botaoAdicionarConfiguracao = findViewById(R.id.addConfigButton)

        carregarConfiguracoes()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, configuracoes)
        listaConfiguracoes.adapter = adapter

        botaoAdicionarConfiguracao.setOnClickListener {
            // Ação para adicionar nova configuração, futura feature
        }
    }

    private fun carregarConfiguracoes() {
        val sharedPref = getSharedPreferences("IrisConfig", MODE_PRIVATE)
        val configuracoesSalvas = sharedPref.all
        configuracoes.clear()
        for (entry in configuracoesSalvas) {
            configuracoes.add("${entry.key}: ${entry.value}")
        }
    }
}
