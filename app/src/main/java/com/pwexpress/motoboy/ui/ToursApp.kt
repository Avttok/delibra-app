package com.pwexpress.motoboy.ui

import com.pwexpress.core.designsystem.PassoTour

/** Os tours do app (o mesmo texto da central de ajuda, em pílulas). */
object ToursApp {
    val motoboy = listOf(
        PassoTour(null, "Bem-vindo ao app", "Um passeio rápido pelo Início. Dá para rever quando quiser, no menu da sua foto."),
        PassoTour("status", "Online e offline", "Segure o botão até o anel encher para ficar online e receber rotas. Para sair, segure de novo."),
        PassoTour("raio", "Raio e GPS", "Aqui ficam o sinal do GPS e o seu raio: até onde você aceita ir buscar uma coleta. Toque no raio para mudar."),
        PassoTour("dia", "Seu dia", "Entregas, rotas e quilômetros de hoje e do período do holerite."),
        PassoTour("abas", "Rotas e Holerite", "Em Rotas ficam as rotas abertas para pegar e as planejadas para reservar. No Holerite, os valores de cada período."),
        PassoTour("conta", "Seu menu", "Perfil e medalhas, cores do app, configurações do aparelho, a central de ajuda e este tour."),
    )
    val gestor = listOf(
        PassoTour(null, "Bem-vindo ao painel no app", "Tudo o que o site do gestor faz, no celular. Dá para rever este tour no menu da sua foto."),
        PassoTour("abas-gestor", "As abas", "Mapa ao vivo, Pedidos (pedidos e rotas), Pessoas (clientes e equipe), Carteira e Ajustes, com as regras do sistema."),
        PassoTour("conta", "Seu menu", "Modo teste para treinar sem mexer no real, cores, a central de ajuda e este tour. Quem também entrega troca para o MotoApp no topo."),
    )
}
