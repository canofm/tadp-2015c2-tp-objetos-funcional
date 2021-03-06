package ar.edu.tadp.dragonball

package object Movimientos {
  type Guerreros = (Guerrero, Guerrero)

  abstract class Movimiento {
    def movimiento(guerreros: Guerreros): Guerreros
    def apply(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      (atacante.estado, this) match {
        case (Muerto, _) => guerreros
        case (KO, UsarItem(SemillaDelErmitanio)) => movimiento(guerreros)
        case (KO, _) => guerreros
        case (Luchando, _) => movimiento(guerreros)
        case (Fajado(_), DejarseFajar) => movimiento(guerreros)
        case (Fajado(_), Genkidama) => movimiento(guerreros)
        case (Fajado(_), _) => movimiento(atacante estas Luchando, oponente)
      }
    }
  }

  case object DejarseFajar extends Movimiento {
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      atacante.estado match {
        case Luchando => (atacante estas Fajado(1), oponente)
        case Fajado(rounds) => (atacante estas Fajado(rounds + 1), oponente)
        case _ => guerreros
      }
    }
  }

  case object CargarKi extends Movimiento {
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      atacante.especie match {
        case Saiyajin(SuperSaiyajin(nivel), _) => (atacante actualizarEnergia (150 * nivel), oponente)
        case Androide => guerreros
        case _ => (atacante actualizarEnergia 100, oponente)
      }
    }
  }

  case object ComerseAlOponente extends Movimiento {
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      atacante.especie match {
        case Monstruo(tipoDigestion, guerrerosComidos) if oponente.energia < atacante.energia =>
          (atacante.comerseA(oponente, tipoDigestion, guerrerosComidos), oponente.estas(Muerto))
        case _ => guerreros
      }
    }
  }

  case class UsarItem(item: Item) extends Movimiento {
    def quedarKOSiEnergiaMenorA300(guerrero: Guerrero) = if (guerrero.energia < 300) guerrero.estas(KO) else guerrero
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      (item, oponente.especie, oponente.estado) match {
        case (ArmaRoma, Androide, _) => (atacante, oponente)
        case (ArmaRoma, _, _) => (atacante, quedarKOSiEnergiaMenorA300(oponente))
        case (ArmaFilosa, Saiyajin(MonoGigante, _), _) =>
          (atacante, oponente.cambiarEspecieA(Saiyajin(Normal, cola = false)).cambiarEnergiaA(1).estas(KO))
        case (ArmaFilosa, Saiyajin(estado, tieneCola), _) if tieneCola =>
          (atacante, oponente.cambiarEspecieA(Saiyajin(estado, cola = false)).cambiarEnergiaA(1))
        case (ArmaFilosa, _, _) => (atacante, oponente actualizarEnergia -(atacante.energia / 100))
        //TODO: Restar una municion del inventario
        case (ArmaDeFuego, Humano, _) => (atacante, oponente actualizarEnergia -20)
        case (ArmaDeFuego, Namekusein, KO) => (atacante, oponente actualizarEnergia -10)
        case (SemillaDelErmitanio, _, _) => (atacante.recuperarEnergiaMaxima.eliminarItem(item), oponente)
        case (_,_,_) => (atacante, oponente)
      }
    }
  }

  case object ConvertirseEnMono extends Movimiento {
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      atacante.especie match {
        case Saiyajin(MonoGigante, _) => (atacante, oponente)
        case Saiyajin(_, tieneCola) if tieneCola && atacante.tieneItem(FotoDeLaLuna) =>
          (atacante.cambiarEstadoSaiyajin(MonoGigante,tieneCola).recuperarEnergiaMaxima.multiplicarEnergiaMaximaPor(3), oponente)
        case _ => (atacante, oponente)
      }
    }
  }

  case object ConvertirseEnSuperSaiyajin extends Movimiento {
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      atacante.especie match {
        case Saiyajin(MonoGigante, _) => (atacante, oponente)
        case Saiyajin(estado, tieneCola) if atacante.puedeSubirDeNivel() =>
          (atacante.cambiarEstadoSaiyajin(SuperSaiyajin(estado.proximoNivelZ),tieneCola).multiplicarEnergiaMaximaPor(5), oponente)
        case _ => (atacante, oponente)
      }
    }
  }

  case class Fusionarse(guerreroParaFusionar: Guerrero) extends Movimiento  {
    override def movimiento(guerreros: Guerreros) = {
      val (amigo, oponente) = guerreros
      (amigo.especie, guerreroParaFusionar.especie) match {
        case (_:Fusionable, _:Fusionable) =>
          val nuevoGuerrero: Guerrero =
            guerreroParaFusionar.cambiarEnergiaMaximaA(amigo.energiaMaxima + amigo.energiaMaxima)
              .cambiarEnergiaA(amigo.energia + guerreroParaFusionar.energia)
              .agregarMovimientos(amigo.movimientosPropios)
              .cambiarEspecieA(Fusion)
          (nuevoGuerrero, oponente)
        case _ => (guerreroParaFusionar, amigo)
      }
    }
  }
  
  case class Magia(paseDeMagia: Guerreros => Guerreros) extends Movimiento {
    override def movimiento(guerrreros: Guerreros) = {
      val(atacante, oponente): Guerreros = guerrreros
      atacante.especie match {
       case _:Magico => paseDeMagia(guerrreros)
       case _ if (atacante.tieneLas7Esferas) => paseDeMagia (atacante.eliminarEsferas, oponente)
       case _ => guerrreros
     }
   }
  }


  trait TipoAtaque
  case object Fisico extends TipoAtaque
  case object Energia extends TipoAtaque

  abstract class Ataque(tipoAtaque: TipoAtaque) extends Movimiento {
    def ataque(atacante: Guerrero, oponente: Guerrero): (Int, Int)
    override def movimiento(guerreros: Guerreros) = {
      val (atacante, oponente) = guerreros
      val (danioAtacante, danioOponente) = ataque(atacante, oponente)
      (oponente.especie,tipoAtaque) match {
        case (Androide, Energia) => (atacante actualizarEnergia danioAtacante, oponente actualizarEnergia Math.abs(danioOponente))
        case _ => (atacante actualizarEnergia danioAtacante, oponente actualizarEnergia danioOponente)
      }
    }
  }

  case object MuchosGolpesNinja extends Ataque(Fisico) {
    override def ataque(atacante: Guerrero, oponente: Guerrero) = {
      (atacante.especie, oponente.especie) match {
        case (Humano, Androide) => (-10, 0)
        case _ => if (atacante.energia >= oponente.energia) (0,-20) else (-20,0)
      }
    }
  }

  case object Explotar extends Ataque(Fisico) {
    override def ataque(atacante: Guerrero, oponente: Guerrero) = {
      atacante.especie match {
        case Androide | Monstruo(_,_) => explotar(atacante, oponente)
        case _ => (0, 0)
      }
    }

    def explotar(atacante: Guerrero, oponente: Guerrero) = {
      val factor = atacante.especie match {
        case Androide => 3
        case _ => 2
      }
      val danioRecibido = oponente.energia - atacante.energia*factor
      oponente.especie match {
        case Namekusein =>
          if (danioRecibido <= 0) (-atacante.energia,-(oponente.energia-1))
          else (-atacante.energia, -Math.abs(danioRecibido))
        case _ => (-atacante.energia, -Math.abs(danioRecibido))
      }
    }
  }

  case class Onda(energiaNecesaria: Int) extends Ataque(Energia) {
    override def ataque(atacante: Guerrero, oponente: Guerrero) = {
      if (atacante.energia < energiaNecesaria) (0,0)
      else oponente.especie match {
        case Monstruo(_,_) => (-energiaNecesaria, -(energiaNecesaria / 2))
        case _ => (-energiaNecesaria, -(energiaNecesaria * 2))
      }
    }
  }

  case object Genkidama extends Ataque(Energia) {
    override def ataque(atacante: Guerrero, oponente: Guerrero) = {
      atacante.estado match {
        case Fajado(rounds) => (0, -Math.pow(10, rounds).toInt)
        case _ => (0, -10)
      }
    }
  }

}
